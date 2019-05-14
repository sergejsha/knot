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
    private var reducer: Reducer<State, Change, Action>? = null
    private val eventTransformers = mutableListOf<EventTransformer<Change>>()
    private val actionTransformers = mutableListOf<ActionTransformer<Action, Change>>()
    private val stateInterceptors = mutableListOf<Interceptor<State>>()
    private val changeInterceptors = mutableListOf<Interceptor<Change>>()
    private val actionInterceptors = mutableListOf<Interceptor<Action>>()

    /** A section for [State] and [Change] related declarations. */
    fun state(block: StateBuilder<State>.() -> Unit) {
        StateBuilder(stateInterceptors)
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
                reducer = it.reducer
                reduceOn = it.reduceOn
            }
    }

    /** A section for [Action] related declarations. */
    fun actions(block: ActionsBuilder<Change, Action>.() -> Unit) {
        ActionsBuilder(actionTransformers, actionInterceptors).also(block)
    }

    /** A section for *Event* related declarations. */
    fun events(block: EventsBuilder<Change>.() -> Unit) {
        EventsBuilder(eventTransformers).also(block)
    }

    /** A section for declaring interceptors of [State], [Change] or [Action]. */
    fun intercept(block: InterceptBuilder<State, Change, Action>.() -> Unit) {
        InterceptBuilder(stateInterceptors, changeInterceptors, actionInterceptors).also(block)
    }

    /** A section for declaring watchers of [State], [Change] or [Action]. */
    fun watch(block: WatchBuilder<State, Change, Action>.() -> Unit) {
        WatchBuilder(stateInterceptors, changeInterceptors, actionInterceptors).also(block)
    }

    internal fun build(): Knot<State, Change, Action> = DefaultKnot(
        initialState = checkNotNull(initialState) { "state { initial } must be set" },
        observeOn = observeOn,
        reduceOn = reduceOn,
        reducer = checkNotNull(reducer) { "changes { reduce } must be set" },
        eventTransformers = eventTransformers,
        actionTransformers = actionTransformers,
        stateInterceptors = stateInterceptors,
        changeInterceptors = changeInterceptors,
        actionInterceptors = actionInterceptors
    )

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
         * New `State` and `Action` can be joined together using overloaded [State.plus()]
         * operator. For returning `State` without action call *.only* on the state.
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

        /** A function for intercepting [Change] emissions. */
        fun intercept(interceptor: Interceptor<Change>) {
            changeInterceptors += interceptor
        }

        /** A function for watching [Change] emissions. */
        fun watchAll(watcher: Watcher<Change>) {
            changeInterceptors += WatchingInterceptor(watcher)
        }

        /** A function for watching emissions of all `Changes`. */
        inline fun <reified T : Change> watch(noinline watcher: Watcher<T>) {
            watchAll(TypedWatcher(T::class.java, watcher))
        }

        /** Turns [State] into an [Effect] without [Action]. */
        val State.only: Effect<State, Action> get() = Effect(this)

        /** Combines [State] and [Action] into [Effect]. */
        operator fun State.plus(action: Action) = Effect(this, action)

        /** Throws [IllegalStateException] with current [State] and given [Change] in its message. */
        fun State.unexpected(change: Change): Nothing = error("Unexpected $change in $this")
    }
}

@KnotDsl
class StateBuilder<State : Any>
internal constructor(
    private val stateInterceptors: MutableList<Interceptor<State>>
) {
    /** Mandatory initial [State] of the [Knot]. */
    var initial: State? = null

    /** An optional [Scheduler] used for dispatching state changes. */
    var observeOn: Scheduler? = null

    /** A function for intercepting [State] mutations. */
    fun intercept(interceptor: Interceptor<State>) {
        stateInterceptors += interceptor
    }

    /** A function for watching mutations of any [State]. */
    fun watchAll(watcher: Watcher<State>) {
        stateInterceptors += WatchingInterceptor(watcher)
    }

    /** A function for watching mutations of all `States`. */
    inline fun <reified T : State> watch(noinline watcher: Watcher<T>) {
        watchAll(TypedWatcher(T::class.java, watcher))
    }
}

@KnotDsl
class ActionsBuilder<Change : Any, Action : Any>
internal constructor(
    private val actionTransformers: MutableList<ActionTransformer<Action, Change>>,
    private val actionInterceptors: MutableList<Interceptor<Action>>
) {

    /** A function used for declaring an [ActionTransformer] function. */
    fun performAll(transformer: ActionTransformer<Action, Change>) {
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
        performAll(TypedActionTransformer(A::class.java, transformer))
    }

    /** A function for intercepting [Action] emissions. */
    fun intercept(interceptor: Interceptor<Action>) {
        actionInterceptors += interceptor
    }

    /** A function for watching [Action] emissions. */
    fun watchAll(watcher: Watcher<Action>) {
        actionInterceptors += WatchingInterceptor(watcher)
    }

    /** A function for watching emissions of all `Changes`. */
    inline fun <reified T : Action> watch(noinline watcher: Watcher<T>) {
        watchAll(TypedWatcher(T::class.java, watcher))
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

    /** A function for watching [State] mutations. */
    fun state(watcher: Watcher<State>) {
        stateInterceptors += WatchingInterceptor(watcher)
    }

    /** A function for watching [Change] emissions. */
    fun changes(watcher: Watcher<Change>) {
        changeInterceptors += WatchingInterceptor(watcher)
    }

    /** A function for watching [Action] emissions. */
    fun actions(watcher: Watcher<Action>) {
        actionInterceptors += WatchingInterceptor(watcher)
    }

    /** A function for watching [State] mutations as well as [Change] and [Action] emissions. */
    fun all(watcher: Watcher<Any>) {
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

    /** A function for intercepting [State] mutations. */
    fun state(interceptor: Interceptor<State>) {
        stateInterceptors += interceptor
    }

    /** A function for intercepting [Change] emissions. */
    fun changes(interceptor: Interceptor<Change>) {
        changeInterceptors += interceptor
    }

    /** A function for intercepting [Action] emissions. */
    fun actions(interceptor: Interceptor<Action>) {
        actionInterceptors += interceptor
    }
}

@PublishedApi
internal class TypedActionTransformer<Action : Any, Change : Any, A : Action>(
    private val type: Class<A>,
    private val transform: ActionTransformer<A, Change>
) : ActionTransformer<Action, Change> {
    override fun invoke(action: Observable<Action>): Observable<Change> {
        return transform(action.ofType(type))
    }
}

internal class WatchingInterceptor<T>(private val watcher: Watcher<T>) : Interceptor<T> {
    override fun invoke(stream: Observable<T>): Observable<T> = stream.doOnNext(watcher)
}

@PublishedApi
internal class TypedWatcher<Type : Any, T : Type>(
    private val type: Class<T>,
    private val watch: Watcher<T>
) : Watcher<Type> {
    override fun invoke(value: Type) {
        if (type.isInstance(value)) {
            watch(type.cast(value))
        }
    }
}