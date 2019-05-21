package de.halfbit.knot

import io.reactivex.Scheduler
import org.jetbrains.annotations.TestOnly
import kotlin.reflect.KClass

fun <State : Any> compositeKnot(
    block: CompositeKnotBuilder<State>.() -> Unit
): CompositeKnot<State> =
    CompositeKnotBuilder<State>()
        .also(block)
        .build()

@TestOnly
fun <State : Any> testCompositeKnot(
    block: CompositeKnotBuilder<State>.() -> Unit
): TestCompositeKnot<State, Any> =
    CompositeKnotBuilder<State>()
        .also(block)
        .build()

@KnotDsl
class CompositeKnotBuilder<State : Any>
internal constructor() {

    private var initialState: State? = null
    private var observeOn: Scheduler? = null
    private var reduceOn: Scheduler? = null
    private val stateInterceptors = mutableListOf<Interceptor<State>>()
    private val changeInterceptors = mutableListOf<Interceptor<Any>>()
    private val actionInterceptors = mutableListOf<Interceptor<Any>>()

    /** A section for [State] related declarations. */
    fun state(block: StateBuilder<State>.() -> Unit) {
        StateBuilder(stateInterceptors)
            .also {
                block(it)
                initialState = it.initial
                observeOn = it.observeOn
            }
    }

    /** A section for [Change] related declarations. */
    fun changes(block: ChangesBuilder<Any>.() -> Unit) {
        ChangesBuilder(changeInterceptors)
            .also {
                block(it)
                reduceOn = it.reduceOn
            }
    }


    internal fun build() = DefaultCompositeKnot(
        initialState = checkNotNull(initialState) { "state { initial } must be set" },
        observeOn = observeOn,
        reduceOn = reduceOn,
        stateInterceptors = stateInterceptors,
        changeInterceptors = changeInterceptors,
        actionInterceptors = actionInterceptors
    )

    @KnotDsl
    class ChangesBuilder<Change : Any>
    internal constructor(
        private val changeInterceptors: MutableList<Interceptor<Change>>
    ) {
        /** An optional [Scheduler] used for reduce function. */
        var reduceOn: Scheduler? = null

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
    }
}

@KnotDsl
class PrimeBuilder<State : Any, Change : Any, Action : Any>
internal constructor(
    private val reducers: MutableMap<KClass<out Change>, Reducer<State, Change, Action>>,
    private val eventSources: MutableList<EventSource<Change>>,
    private val actionTransformers: MutableList<ActionTransformer<Action, Change>>,
    private val stateInterceptors: MutableList<Interceptor<State>>,
    private val changeInterceptors: MutableList<Interceptor<Change>>,
    private val actionInterceptors: MutableList<Interceptor<Action>>
) {

    /** A section for [State] related declarations. */
    fun state(block: StateBuilder<State>.() -> Unit) {
        StateBuilder(stateInterceptors).also(block)
    }

    /** A section for [Change] related declarations. */
    fun changes(block: ChangesBuilder<State, Change, Action>.() -> Unit) {
        ChangesBuilder(reducers, changeInterceptors).also(block)
    }

    /** A section for [Action] related declarations. */
    fun actions(block: ActionsBuilder<Change, Action>.() -> Unit) {
        ActionsBuilder(actionTransformers, actionInterceptors).also(block)
    }

    /** A section for *Event* related declarations. */
    fun events(block: EventsBuilder<Change>.() -> Unit) {
        EventsBuilder(eventSources).also(block)
    }

    /** A section for declaring interceptors of [State], [Change] or [Action]. */
    fun intercept(block: InterceptBuilder<State, Change, Action>.() -> Unit) {
        InterceptBuilder(stateInterceptors, changeInterceptors, actionInterceptors).also(block)
    }

    /** A section for declaring watchers of [State], [Change] or [Action]. */
    fun watch(block: WatchBuilder<State, Change, Action>.() -> Unit) {
        WatchBuilder(stateInterceptors, changeInterceptors, actionInterceptors).also(block)
    }

    @KnotDsl
    class StateBuilder<State : Any>
    internal constructor(
        private val stateInterceptors: MutableList<Interceptor<State>>
    ) {

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
    class ChangesBuilder<State : Any, Change : Any, Action : Any>
    internal constructor(
        private val reducers: MutableMap<KClass<out Change>, Reducer<State, Change, Action>>,
        private val changeInterceptors: MutableList<Interceptor<Change>>
    ) {

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
        fun reduce(changeType: KClass<out Change>, reduce: Reducer<State, Change, Action>) {
            reducers[changeType] = reduce
        }

        inline fun <reified C : Change> reduce(noinline reduce: Reducer<State, C, Action>) {
            @Suppress("UNCHECKED_CAST")
            reduce(C::class, reduce as Reducer<State, Change, Action>)
        }

        /** A function for intercepting [Change] emissions. */
        fun intercept(interceptor: Interceptor<Change>) {
            changeInterceptors += interceptor
        }

        /** A function for watching [Change] emissions. */
        fun watch(watcher: Watcher<Change>) {
            changeInterceptors += WatchingInterceptor(watcher)
        }

        /** Turns [State] into an [Effect] without [Action]. */
        val State.only: Effect<State, Action> get() = Effect(this)

        /** Combines [State] and [Action] into [Effect]. */
        operator fun State.plus(action: Action) = Effect(this, action)

        /** Throws [IllegalStateException] with current [State] and given [Change] in its message. */
        fun State.unexpected(change: Change): Nothing = error("Unexpected $change in $this")
    }
}