package de.halfbit.knot

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject


/**
 * Knot helps managing application state by reacting on events and performing asynchronous
 * actions in a structured way. There are five core concepts Knot defines: [State], [Change],
 * [Reducer], [Effect] and `Action`.
 *
 * [Flowchart diagram](https://github.com/beworker/knot/raw/master/docs/diagrams/flowchart-knot.png)
 *
 * [State] represents an immutable partial state of an Android application. It can be a state
 * of a screen or a state of an internal headless component, like repository.
 *
 * [Change] is an immutable data object with an optional payload intended for changing the `State`.
 * A `Change` can be produced from an external event or be a result of execution of an `Action`.
 *
 * `Action` is a synchronous or an asynchronous operation which, when completed, can emit a new `Change`.
 *
 * [Reducer] is a pure function that takes the previous `State` and a `Change` as arguments and returns
 * the new `State` and an optional `Action` wrapped by `Effect` class. `Reducer` in Knot is designer
 * to stays side-effects free because each side-effect can be turned into an `Action` and returned from
 * `Reducer` function together with a new `State`.
 *
 * [Effect] is a convenient wrapper class containing the new `State` and an optional `Action`. If
 * `Action` is present, Knot will perform it and provide resulting `Change` back to `Reducer`.
 *
 * Example below shows the Knot which is capable of loading data, handling success and failure
 * loading results and reloading data when an external "data changed" signal is received.
 * ```
 *  val knot = knot {
 *      state {
 *          initial = State.Empty
 *      }
 *      changes {
 *          reduce { change ->
 *              when (change) {
 *                  is Change.Load -> State.Loading + Action.Load
 *                  is Change.Load.Success -> State.Content(data).only
 *                  is Change.Load.Failure -> State.Failed(error).only
 *              }
 *          }
 *      }
 *      actions {
 *          perform<Action.Load> { action ->
 *              action
 *                  .switchMapSingle<String> { api.load() }
 *                  .map<Change> { Change.Load.Success(it) }
 *                  .onErrorReturn { Change.Load.Failure(it) }
 *              }
 *          }
 *      }
 *      events {
 *          transform {
 *              dataChangeObserver.signal.map { Change.Load }
 *          }
 *      }
 *      watch {
 *          all { println(it) }
 *      }
 *  }
 *
 *  knot.change.accept(Change.Load)
 * ```
 */
interface Knot<State : Any, Change : Any> : Store<State> {

    /** Change emitter used for delivering changes to this knot. */
    val change: Consumer<Change>
}

/** Store is a disposable container for a [State]. */
interface Store<State : Any> : Disposable {

    /** Observable state. */
    val state: Observable<State>
}

/** Convenience wrapper around [State] and optional [Action]. */
sealed class Effect<State : Any, Action : Any> {

    /** Adds another action to [Effect]. */
    abstract operator fun plus(action: Action?): Effect<State, Action>

    data class WithAction<State : Any, Action : Any>(
        val state: State,
        val action: Action? = null
    ) : Effect<State, Action>() {
        override fun plus(action: Action?): Effect<State, Action> =
            when {
                action == null -> this
                this.action == null -> WithAction(state, action)
                else -> WithActions(state, listOf(this.action, action))
            }
    }

    data class WithActions<State : Any, Action : Any>(
        val state: State,
        val actions: List<Action>
    ) : Effect<State, Action>() {
        override fun plus(action: Action?): Effect<State, Action> =
            if (action == null) this
            else WithActions(state, actions + action)
    }
}

/** A function accepting the `State` and a `Change` and returning a new `State`. */
typealias Reducer<State, Change, Action> = State.(change: Change) -> Effect<State, Action>

/** A function returning an [Observable] `Change`. */
typealias EventSource<Change> = () -> Observable<Change>

/** A function used for performing given `Action` and emitting resulting `Change` or *Changes*. */
typealias ActionTransformer<Action, Change> = (action: Observable<Action>) -> Observable<Change>

/** A function used for performing given `Action` and emitting resulting `Change` or *Changes*. */
typealias ActionTransformerWithReceiver<Action, Change> = Observable<Action>.() -> Observable<Change>

/** A function used for intercepting events of given type. */
typealias Interceptor<Type> = (value: Observable<Type>) -> Observable<Type>

/** A function used for consuming events of given type. */
typealias Watcher<Type> = (value: Type) -> Unit

internal class DefaultKnot<State : Any, Change : Any, Action : Any>(
    initialState: State,
    observeOn: Scheduler?,
    reduceOn: Scheduler?,
    reducer: Reducer<State, Change, Action>,
    coldEventSources: Lazy<List<EventSource<Change>>>,
    eventSources: List<EventSource<Change>>,
    actionTransformers: List<ActionTransformer<Action, Change>>,
    stateInterceptors: List<Interceptor<State>>,
    changeInterceptors: List<Interceptor<Change>>,
    actionInterceptors: List<Interceptor<Action>>
) : Knot<State, Change> {

    private val changeSubject = PublishSubject.create<Change>()
    private val actionSubject = PublishSubject.create<Action>()
    private val disposables = CompositeDisposable()

    private var subscriberCount: Int = 0
    private var coldEventsObservable: Observable<Change>? = null
    private var coldEventsDisposable: Disposable? = null

    override fun isDisposed(): Boolean = disposables.isDisposed
    override fun dispose() = disposables.dispose()

    override val change: Consumer<Change> = Consumer { changeSubject.onNext(it) }
    override val state: Observable<State> = Observable
        .merge(
            mutableListOf<Observable<Change>>().apply {
                this += changeSubject

                actionSubject
                    .intercept(actionInterceptors)
                    .bind(actionTransformers) { this += it }

                eventSources
                    .map { transform -> this += transform() }
            }
        )
        .let { stream -> reduceOn?.let { stream.observeOn(it) } ?: stream }
        .serialize()
        .intercept(changeInterceptors)
        .scan(initialState) { state, change -> reducer(state, change).emitActions(actionSubject) }
        .distinctUntilChanged { prev, curr -> prev === curr }
        .intercept(stateInterceptors)
        .let { stream -> observeOn?.let { stream.observeOn(it) } ?: stream }
        .replay(1)
        .also {
            if (coldEventSources.isInitialized()) {
                coldEventsObservable = coldEventSources.mergeIntoObservable()
            }
            disposables.add(it.connect())
        }
        .doOnSubscribe(::doOnSubscribe)
        .doFinally(::doFinally)

    @Synchronized
    private fun doOnSubscribe(disposable: Disposable) {
        if (subscriberCount++ == 0) {
            coldEventsObservable?.let {
                check(coldEventsDisposable == null)
                coldEventsDisposable = it.subscribe(
                    changeSubject::onNext,
                    changeSubject::onError
                )
            }
        }
    }

    @Synchronized
    private fun doFinally() {
        if (--subscriberCount == 0) {
            coldEventsDisposable?.let {
                it.dispose()
                coldEventsDisposable = null
            }
        }
    }
}

internal fun <State : Any, Action : Any> Effect<State, Action>.emitActions(
    actionSubject: Subject<Action>
): State = when (this) {
    is Effect.WithAction -> {
        action?.let { action -> actionSubject.onNext(action) }
        state
    }
    is Effect.WithActions -> {
        for (action in actions) actionSubject.onNext(action)
        state
    }
}

internal fun <T> Observable<T>.intercept(interceptors: List<Interceptor<T>>): Observable<T> =
    interceptors.fold(this) { state, intercept -> intercept(state) }

internal fun <Action, Change> Observable<Action>.bind(
    actionTransformers: List<ActionTransformer<Action, Change>>,
    append: (observable: Observable<Change>) -> Unit
) {
    if (actionTransformers.isEmpty()) append(flatMap { Observable.empty<Change>() })
    else share().let { shared -> actionTransformers.map { transform -> append(transform(shared)) } }
}

internal fun <Change> Lazy<List<EventSource<Change>>>.mergeIntoObservable(): Observable<Change> =
    Observable.merge(
        mutableListOf<Observable<Change>>().apply {
            value.map { source -> this += source() }
        }
    )
