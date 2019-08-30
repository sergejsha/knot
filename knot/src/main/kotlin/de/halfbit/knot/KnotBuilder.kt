package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler

/** Creates a [Knot] instance. */
fun <State : Any, Change : Any, Action : Any> knot(
    block: KnotBuilder<State, Change, Action>.() -> Unit
): Knot<State, Change> =
    KnotBuilder<State, Change, Action>()
        .also(block)
        .build()

@DslMarker
annotation class KnotDsl

/** A configuration builder for a [Knot]. */
@KnotDsl
class KnotBuilder<State : Any, Change : Any, Action : Any>
internal constructor() {

    private var initialState: State? = null
    private var observeOn: Scheduler? = null
    private var reduceOn: Scheduler? = null
    private var reducer: Reducer<State, Change, Action>? = null
    private val eventSources = mutableListOf<EventSource<Change>>()
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
        EventsBuilder(eventSources).also(block)
    }

    internal fun build(): Knot<State, Change> = DefaultKnot(
        initialState = checkNotNull(initialState) { "state { initial } must be declared" },
        observeOn = observeOn,
        reduceOn = reduceOn,
        reducer = checkNotNull(reducer) { "changes { reduce } must be declared" },
        eventSources = eventSources,
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

        /** An optional [Scheduler] used for watching *Changes*. */
        var watchOn: Scheduler? = null
            set(value) {
                field = value
                value?.let { changeInterceptors += WatchOnInterceptor(it) }
            }

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
            changeInterceptors += WatchingInterceptor(watcher, watchOn)
        }

        /** A function for watching emissions of all `Changes`. */
        inline fun <reified T : Change> watch(noinline watcher: Watcher<T>) {
            watchAll(TypedWatcher(T::class.java, watcher))
        }

        /** Turns [State] into an [Effect] without [Action]. */
        val State.only: Effect<State, Action> get() = Effect.WithAction(this)

        /** Combines [State] and [Action] into [Effect]. */
        operator fun State.plus(action: Action?) = Effect.WithAction(this, action)

        /** Throws [IllegalStateException] with current [State] and given [Change] in its message. */
        fun State.unexpected(change: Change): Nothing = error("Unexpected $change in $this")
    }
}

/** A configuration builder for [State] related declarations. */
@KnotDsl
class StateBuilder<State : Any>
internal constructor(
    private val stateInterceptors: MutableList<Interceptor<State>>
) {
    /** Mandatory initial [State] of the [Knot]. */
    var initial: State? = null

    /** An optional [Scheduler] used for observing *State* updates. */
    var observeOn: Scheduler? = null

    /** An optional [Scheduler] used for watching *State* updates. */
    var watchOn: Scheduler? = null
        set(value) {
            field = value
            value?.let { stateInterceptors += WatchOnInterceptor(it) }
        }

    /** A function for intercepting [State] mutations. */
    fun intercept(interceptor: Interceptor<State>) {
        stateInterceptors += interceptor
    }

    /** A function for watching mutations of any [State]. */
    fun watchAll(watcher: Watcher<State>) {
        stateInterceptors += WatchingInterceptor(watcher, watchOn)
    }

    /** A function for watching mutations of all `States`. */
    inline fun <reified T : State> watch(noinline watcher: Watcher<T>) {
        watchAll(TypedWatcher(T::class.java, watcher))
    }
}

/** A configuration builder for [Action] related declarations. */
@KnotDsl
class ActionsBuilder<Change : Any, Action : Any>
internal constructor(
    private val actionTransformers: MutableList<ActionTransformer<Action, Change>>,
    private val actionInterceptors: MutableList<Interceptor<Action>>
) {
    /** An optional [Scheduler] used for watching *Actions*. */
    var watchOn: Scheduler? = null
        set(value) {
            field = value
            value?.let { actionInterceptors += WatchOnInterceptor(it) }
        }

    /** A function used for declaring an [ActionTransformer] function. */
    @PublishedApi
    internal fun performAll(transformer: ActionTransformer<Action, Change>) {
        actionTransformers += transformer
    }

    /**
     * A function used for declaring an [ActionTransformer] function for given [Action] type.
     *
     * Example:
     * ```
     *  actions {
     *      perform<Action.Load> {
     *          flatMapSingle<Payload> { api.load() }
     *              .map<Change> { Change.Load.Success(it) }
     *              .onErrorReturn { Change.Load.Failure(it) }
     *      }
     *  }
     * ```
     */
    inline fun <reified A : Action> perform(noinline transformer: ActionTransformerWithReceiver<A, Change>) {
        performAll(TypedActionTransformer(A::class.java, transformer))
    }

    /** A function for intercepting [Action] emissions. */
    fun intercept(interceptor: Interceptor<Action>) {
        actionInterceptors += interceptor
    }

    /** A function for watching [Action] emissions. */
    fun watchAll(watcher: Watcher<Action>) {
        actionInterceptors += WatchingInterceptor(watcher, watchOn)
    }

    /** A function for watching emissions of all `Changes`. */
    inline fun <reified T : Action> watch(noinline watcher: Watcher<T>) {
        watchAll(TypedWatcher(T::class.java, watcher))
    }
}

/** A configuration builder for `Events` related declarations. */
@KnotDsl
class EventsBuilder<Change : Any>
internal constructor(
    private val eventSources: MutableList<EventSource<Change>>
) {

    /**
     * A function used for turning an external observable *Event* into a [Change].
     *
     * Example:
     * ```
     *  events {
     *      source {
     *          userLocationObserver
     *              .map { Change.UserLocationUpdated(it) }
     *      }
     *  }
     * ```
     */
    fun source(source: EventSource<Change>) {
        eventSources += source
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

internal class WatchingInterceptor<T>(
    private val watcher: Watcher<T>,
    private val watchOn: Scheduler?
) : Interceptor<T> {
    override fun invoke(stream: Observable<T>): Observable<T> =
        stream.let { if (watchOn != null) it.observeOn(watchOn) else it }.doOnNext(watcher)
}

internal class WatchOnInterceptor<T>(
    private val watchOn: Scheduler
) : Interceptor<T> {
    override fun invoke(stream: Observable<T>): Observable<T> =
        stream.observeOn(watchOn)
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

internal const val WATCH_ON_ERROR = "'watchOn' must be defined just once and before any watching function."