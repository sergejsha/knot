package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject

interface Knot<State : Any, Change : Any> {
    val state: Observable<State>
    val change: Consumer<Change>
    fun dispose()
}

interface WithEffect<State : Any, Change : Any> {
    fun effect(state: State, action: Single<Change>? = null): Effect<State, Change>
}

internal class DefaultKnot<State : Any, Change : Any>(
    initialState: State,
    reducer: Reducer<State, Change>,
    observeOn: Scheduler?,
    reduceOn: Scheduler?,
    eventTransformers: List<EventTransformer<Change>>
) : Knot<State, Change> {

    private val changeSubject = PublishSubject.create<Change>()
    private val actionSubject = PublishSubject.create<Single<Change>>()
    private var disposable: Disposable? = null

    private val withEffect = object : WithEffect<State, Change> {
        override fun effect(state: State, action: Single<Change>?) = Effect(state, action)
    }

    override val change: Consumer<Change> = Consumer { changeSubject.onNext(it) }
    override val state: Observable<State> = Observable
        .merge(
            mutableListOf<Observable<Change>>().also { observables ->
                observables.add(changeSubject)
                eventTransformers.map { observables.add(it.invoke()) }
                observables.add(actionSubject.flatMapSingle { it })
            }
        )
        .let { change -> reduceOn?.let { change.observeOn(it) } ?: change }
        .serialize()
        .scan(initialState) { state, change ->
            reducer
                .invoke(withEffect, change, state)
                .also { effect -> effect.action?.let { action -> actionSubject.onNext(action) } }
                .state
        }
        .let { state -> observeOn?.let { state.observeOn(it) } ?: state }
        .distinctUntilChanged()
        .replay(1)
        .also { disposable = it.connect() }

    override fun dispose() {
        disposable?.dispose()
    }

}
