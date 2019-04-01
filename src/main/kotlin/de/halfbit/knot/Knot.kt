package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicReference

interface Knot<State : Any, Command : Any, Change : Any> {
    val state: Observable<State>
    val command: Consumer<Command>
    val currentState: State
    fun dispose()
}

interface WithEffect<State : Any, Change : Any> {
    fun effect(state: State, action: Single<Change>? = null): Effect<State, Change>
}

internal class DefaultKnot<State : Any, Command : Any, Change : Any>(
    initialState: State,
    reducer: Reducer<State, Change>,
    observeOn: Scheduler?,
    reduceOn: Scheduler?,
    eventTransformers: List<EventTransformer<Change>>,
    commandTransformers: List<CommandTransformer<Command, Change>>
) : Knot<State, Command, Change> {

    private val commandSubject = PublishSubject.create<Command>()
    private val actionSubject = PublishSubject.create<Single<Change>>()
    private val stateValue = AtomicReference<State>(initialState)
    private var disposable: Disposable? = null

    private val withEffect = object : WithEffect<State, Change> {
        override fun effect(state: State, action: Single<Change>?) = Effect(state, action)
    }

    override val command: Consumer<Command> = Consumer { commandSubject.onNext(it) }
    override val currentState: State get() = stateValue.get()
    override val state: Observable<State> = Observable
        .merge(
            mutableListOf<Observable<Change>>().also { observables ->
                eventTransformers.map { observables.add(it.invoke()) }
                commandTransformers.map { observables.add(it.invoke(commandSubject)) }
                observables.add(actionSubject.flatMapSingle { it })
            }
        )
        .let { change -> reduceOn?.let { change.observeOn(it) } ?: change }
        .serialize()
        .map { change ->
            reducer
                .invoke(withEffect, change, stateValue.get())
                .also { effect ->
                    stateValue.set(effect.state)
                    effect.action?.let { action -> actionSubject.onNext(action) }
                }
                .state
        }
        .let { state -> observeOn?.let { state.observeOn(it) } ?: state }
        .startWith(initialState)
        .distinctUntilChanged()
        .replay(1)
        .also { disposable = it.connect() }

    override fun dispose() {
        disposable?.dispose()
    }

}
