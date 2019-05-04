package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject

interface Knot<State : Any, Change : Any, Action : Any> {
    val state: Observable<State>
    val change: Consumer<Change>
    val disposable: Disposable
}

class Effect<State : Any, Action : Any>(
    val state: State,
    val action: Action? = null
)

internal class DefaultKnot<State : Any, Change : Any, Action : Any>(
    initialState: State,
    reducer: Reducer<State, Change, Action>,
    observeOn: Scheduler?,
    reduceOn: Scheduler?,
    eventTransformers: List<EventTransformer<Change>>,
    actionTransformers: List<ActionTransformer<Action, Change>>
) : Knot<State, Change, Action> {

    private val changeSubject = PublishSubject.create<Change>()
    private val actionSubject = PublishSubject.create<Action>()

    override val disposable = CompositeDisposable()
    override val change: Consumer<Change> = Consumer { changeSubject.onNext(it) }
    override val state: Observable<State> = Observable
        .merge(
            mutableListOf<Observable<Change>>()
                .also { observables ->
                    observables.add(changeSubject)
                    eventTransformers.map { observables.add(it.invoke()) }
                    actionTransformers.map { observables.add(it.invoke(actionSubject).toObservable()) }
                }
        )
        .let { change -> reduceOn?.let { change.observeOn(it) } ?: change }
        .serialize()
        .scan(initialState) { state, change ->
            reducer
                .invoke(change, state)
                .also { it.action?.let { action -> actionSubject.onNext(action) } }
                .state
        }
        .let { state -> observeOn?.let { state.observeOn(it) } ?: state }
        .distinctUntilChanged()
        .replay(1)
        .also { disposable.add(it.connect()) }
}
