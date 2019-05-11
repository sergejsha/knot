package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler

/** Creates a [Knot] instance. */
fun <State : Any, Change : Any, Action : Any> knot(
    block: KnotBuilder<State, Change, Action>.() -> Unit
): Knot<State, Change, Action> =
    KnotBuilder<State, Change, Action>()
        .also(block)
        .build()

@DslMarker
annotation class KnotDsl

@KnotDsl
class KnotBuilder<State : Any, Change : Any, Action : Any>
internal constructor() {
    private var initialState: State? = null
    private var observeOn: Scheduler? = null
    private var reduceOn: Scheduler? = null
    private var reduce: Reducer<State, Change, Action>? = null
    private val eventTransformers = mutableListOf<EventTransformer<Change>>()
    private val actionTransformers = mutableListOf<ActionTransformer<Action, Change>>()
    private val stateInterceptors = mutableListOf<Interceptor<State>>()
    private val changeInterceptors = mutableListOf<Interceptor<Change>>()
    private val actionInterceptors = mutableListOf<Interceptor<Action>>()

    /** A section for [State] and [Change] related declarations. */
    fun state(block: StateBuilder<State, Change, Action>.() -> Unit) {
        StateBuilder<State, Change, Action>(stateInterceptors)
            .also {
                block(it)
                initialState = it.initial
                observeOn = it.observeOn
            }
    }

    /** A section for [Change] related declarations. */
    fun changes(block: ChangesBuilder<State, Change, Action>.() -> Unit) {
        ChangesBuilder<State, Change, Action>(changeInterceptors)
            .also {
                block(it)
                reduce = it.reducer
                reduceOn = it.reduceOn
            }
    }

    /** A section for [Action] related declarations. */
    fun actions(block: ActionsBuilder<Change, Action>.() -> Unit) {
        ActionsBuilder(actionTransformers, actionInterceptors).also(block)
    }

    /** A section for Event related declarations. */
    fun events(block: EventsBuilder<Change>.() -> Unit) {
        EventsBuilder(eventTransformers).also(block)
    }

    fun intercept(block: InterceptBuilder<State, Change, Action>.() -> Unit) {
        InterceptBuilder(stateInterceptors, changeInterceptors, actionInterceptors).also(block)
    }

    fun watch(block: WatchBuilder<State, Change, Action>.() -> Unit) {
        WatchBuilder(stateInterceptors, changeInterceptors, actionInterceptors).also(block)
    }

    fun build(): Knot<State, Change, Action> = DefaultKnot(
        initialState = checkNotNull(initialState) { "knot { state { initial } } must be set" },
        observeOn = observeOn,
        reduceOn = reduceOn,
        reducer = checkNotNull(reduce) { "knot { state { reduce } } must be set" },
        eventTransformers = eventTransformers,
        actionTransformers = actionTransformers,
        stateInterceptors = stateInterceptors,
        changeInterceptors = changeInterceptors,
        actionInterceptors = actionInterceptors
    )

    @KnotDsl
    class StateBuilder<State : Any, Change : Any, Action : Any>
    internal constructor(
        private val stateInterceptors: MutableList<Interceptor<State>>
    ) {
        /** Mandatory initial [State] of the [Knot]. */
        var initial: State? = null

        /** An optional [Scheduler] used for dispatching state changes. */
        var observeOn: Scheduler? = null

        fun intercept(interceptor: Interceptor<State>) {
            stateInterceptors += interceptor
        }

        fun watch(watcher: Watcher<State>) {
            stateInterceptors += WatchingInterceptor(watcher)
        }
    }

    @KnotDsl
    class ChangesBuilder<State : Any, Change : Any, Action : Any>
    internal constructor(
        private val changeInterceptors: MutableList<Interceptor<Change>>
    ) {
        internal var reducer: Reducer<State, Change, Action>? = null

        /** An optional [Scheduler] used for reduce function. */
        var reduceOn: Scheduler? = null

        /**
         * Mandatory reduce function which receives the current [State] and a [Change]
         * and must return [Effect] with a new [State] and an optional [Action].
         *
         * New *State* and *Action* can be joined together using overloaded [State.plus()]
         * operator. For returning *State* without action call *.only* on the state.
         *
         * Example:
         * ```
         *  changes {
         *      reduce { change ->
         *          when (change) {
         *              is Change.Load -> copy(value = "loading") + Action.Load
         *              is Change.Load.Success -> copy(value = change.payload).only
         *              is Change.Load.Failure -> copy(value = "failed").only
         *          }
         *      }
         *  }
         * ```
         */
        fun reduce(reducer: Reducer<State, Change, Action>) {
            this.reducer = reducer
        }

        fun intercept(interceptor: Interceptor<Change>) {
            changeInterceptors += interceptor
        }

        fun watch(watcher: Watcher<Change>) {
            changeInterceptors += WatchingInterceptor(watcher)
        }

        /** Turns [State] into an [Effect] without [Action]. */
        val State.only: Effect<State, Action> get() = Effect(this)

        /** Combines [State] and [Action] into [Effect]. */
        operator fun State.plus(action: Action) = Effect(this, action)
    }

    @KnotDsl
    class ActionsBuilder<Change : Any, Action : Any>
    internal constructor(
        private val actionTransformers: MutableList<ActionTransformer<Action, Change>>,
        private val actionInterceptors: MutableList<Interceptor<Action>>
    ) {

        /** A function used for declaring an [ActionTransformer] function. */
        fun performAny(transformer: ActionTransformer<Action, Change>) {
            actionTransformers += transformer
        }

        /**
         * A function used for declaring an [ActionTransformer] function for given [Action] type.
         *
         * Example:
         * ```
         *  actions {
         *      perform<Action.Load> { action ->
         *          action
         *              .flatMapSingle<Payload> { api.load() }
         *              .map<Change> { Change.Load.Success(it) }
         *              .onErrorReturn { Change.Load.Failure(it) }
         *      }
         *  }
         * ```
         */
        inline fun <reified A : Action> perform(noinline transformer: ActionTransformer<A, Change>) {
            performAny(TypedActionTransformer(A::class.java, transformer))
        }

        fun intercept(interceptor: Interceptor<Action>) {
            actionInterceptors += interceptor
        }

        fun watch(watcher: Watcher<Action>) {
            actionInterceptors += WatchingInterceptor(watcher)
        }
    }

    @KnotDsl
    class EventsBuilder<Change : Any>
    internal constructor(
        private val eventTransformers: MutableList<EventTransformer<Change>>
    ) {

        /**
         * A function used for turning an external observable *Event* into a [Change].
         *
         * Example:
         * ```
         *  events {
         *      transform {
         *          userLocationObserver
         *              .map { Change.UserLocationUpdated(it) }
         *      }
         *  }
         * ```
         */
        fun transform(transformer: EventTransformer<Change>) {
            eventTransformers += transformer
        }
    }

    @KnotDsl
    class WatchBuilder<State : Any, Change : Any, Action : Any>
    internal constructor(
        private val stateInterceptors: MutableList<Interceptor<State>>,
        private val changeInterceptors: MutableList<Interceptor<Change>>,
        private val actionInterceptors: MutableList<Interceptor<Action>>
    ) {

        fun state(watcher: Watcher<State>) {
            stateInterceptors += WatchingInterceptor(watcher)
        }

        fun change(watcher: Watcher<Change>) {
            changeInterceptors += WatchingInterceptor(watcher)
        }

        fun action(watcher: Watcher<Action>) {
            actionInterceptors += WatchingInterceptor(watcher)
        }

        fun any(watcher: Watcher<Any>) {
            stateInterceptors += WatchingInterceptor(watcher as Watcher<State>)
            changeInterceptors += WatchingInterceptor(watcher as Watcher<Change>)
            actionInterceptors += WatchingInterceptor(watcher as Watcher<Action>)
        }
    }

    @KnotDsl
    class InterceptBuilder<State : Any, Change : Any, Action : Any>
    internal constructor(
        private val stateInterceptors: MutableList<Interceptor<State>>,
        private val changeInterceptors: MutableList<Interceptor<Change>>,
        private val actionInterceptors: MutableList<Interceptor<Action>>
    ) {

        fun state(interceptor: Interceptor<State>) {
            stateInterceptors += interceptor
        }

        fun change(interceptor: Interceptor<Change>) {
            changeInterceptors += interceptor
        }

        fun action(interceptor: Interceptor<Action>) {
            actionInterceptors += interceptor
        }
    }
}

/** A function accepting the *State* and a *Change* and returning a new *State*. */
typealias Reducer<State, Change, Action> = State.(change: Change) -> Effect<State, Action>

/** A function returning an [Observable] *Change*. */
typealias EventTransformer<Change> = () -> Observable<Change>

/** A function used for performing given *Action* and emitting resulting *Change* or *Changes*. */
typealias ActionTransformer<Action, Change> = (action: Observable<Action>) -> Observable<Change>

/** A function user for intercepting events of given type. */
typealias Interceptor<Type> = (value: Observable<Type>) -> Observable<Type>

/** A function user for consuming events of given type. */
typealias Watcher<Type> = (value: Type) -> Unit

class TypedActionTransformer<Action : Any, Change : Any, A : Action>(
    private val type: Class<A>,
    private val transform: ActionTransformer<A, Change>
) : ActionTransformer<Action, Change> {
    override fun invoke(action: Observable<Action>): Observable<Change> {
        return transform(action.ofType(type))
    }
}

class WatchingInterceptor<T>(private val watcher: Watcher<T>) : Interceptor<T> {
    override fun invoke(stream: Observable<T>): Observable<T> = stream.doOnNext(watcher)
}
