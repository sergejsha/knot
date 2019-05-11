package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject

/**
 * Knot helps managing application state by reacting on events and performing asynchronous
 * actions in a structured way. There are five core concepts Knot defines: [State], [Change],
 * *Reducer*, [Effect] and [Action].
 *
 * [Flowchart diagram](https://github.com/beworker/knot/raw/master/docs/diagrams/flowchart-knot.png)
 *
 * [State] represents an immutable partial state of an Android application. It can be a state
 * of a screen or a state of an internal headless component, like repository.
 *
 * [Change] is an immutable data object with an optional payload intended for changing the [State].
 * A [Change] can be produced from an external event or be a result of execution of an *Action*.
 *
 * [Action] is a synchronous or an asynchronous operation which, when completed, can emit a new [Change].
 *
 * *Reducer* is a pure function that takes the previous [State] and a [Change] as arguments and returns
 * the new [State] and an optional [Action] wrapped by [Effect] class. *Reducer* in Knot is designer
 * to stays side-effects free because each side-effect can be turned into an [Action] and returned from
 * *Reducer* function together with a new [State].
 *
 * [Effect] is a convenient wrapper class containing the new [State] and an optional [Action]. If
 * [Action] is present, Knot will perform it and provide resulting [Change] back to *Reducer*.
 *
 * Example below shows the Knot which is capable of loading data, handling success and failure
 * loading results and reloading data when an external "data changed" signal is received.
 * ```
 *  val knot = knot {
 *      state {
 *          initial = State.Initial
 *      }
 *      changes {
 *          reduce { change ->
 *              when (change) {
 *                  is Change.Load -> State.Loading.only + Action.Load
 *                  is Change.Load.Success -> State.Content(data).only
 *                  is Change.Load.Failure -> State.Error(error).only
 *              }
 *          }
 *      }
 *      actions {
 *          perform<Action.Load> { action ->
 *              action
 *                  .switchMapSingle<Payload> { api.load() }
 *                  .map<Change> { Change.Load.Success(it) }
 *                  .onErrorReturn { Change.Load.Failure(it) }
 *              }
 *          }
 *      }
 *      events {
 *          transform {
 *              observer.dataChangedSignal.map { Change.Load }
 *          }
 *      }
 *  }
 *
 *  knot.state.subscribe { println(it) }
 * ```
 */
interface Knot<State : Any, Change : Any, Action : Any> {
    val state: Observable<State>
    val change: Consumer<Change>
    val disposable: Disposable
}

/** Convenience wrapper around [State] and optional [Action]. */
class Effect<State : Any, Action : Any>(
    val state: State,
    val action: Action? = null
)

internal class DefaultKnot<State : Any, Change : Any, Action : Any>(
    initialState: State,
    observeOn: Scheduler?,
    reduceOn: Scheduler?,
    reduce: Reduce<State, Change, Action>,
    eventTransformers: List<EventTransformer<Change>>,
    actionTransformers: List<ActionTransformer<Action, Change>>
) : Knot<State, Change, Action> {

    private val changeSubject = PublishSubject.create<Change>()
    private val actionSubject = PublishSubject.create<Action>()

    override val disposable = CompositeDisposable()
    override val change: Consumer<Change> = Consumer { changeSubject.onNext(it) }
    override val state: Observable<State> = Observable
        .merge(
            mutableListOf<Observable<Change>>().apply {
                add(changeSubject)
                eventTransformers.map { add(it.invoke()) }
                actionTransformers.map { add(it.invoke(actionSubject)) }
            }
        )
        .let { change -> reduceOn?.let { change.observeOn(it) } ?: change }
        .serialize()
        .scan(initialState) { state, change ->
            reduce(state, change)
                .also { it.action?.let { action -> actionSubject.onNext(action) } }
                .state
        }
        .let { state -> observeOn?.let { state.observeOn(it) } ?: state }
        .distinctUntilChanged()
        .replay(1)
        .also { disposable.add(it.connect()) }
}