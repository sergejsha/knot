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
    private var reduce: Reduce<State, Change, Action>? = null
    private val eventTransformers = mutableListOf<EventTransformer<Change>>()
    private val actionTransformers = mutableListOf<ActionTransformer<Action, Change>>()

    /** A section for [State] and [Change] related declarations. */
    fun state(block: StateBuilder<State, Change, Action>.() -> Unit) {
        StateBuilder<State, Change, Action>()
            .also {
                block(it)
                initialState = it.initial
                reduce = it.reduce
                observeOn = it.observeOn
                reduceOn = it.reduceOn
            }
    }

    /** A section for [Action] related declarations. */
    fun actions(block: ActionsBuilder<Change, Action>.() -> Unit) {
        ActionsBuilder(actionTransformers).also(block)
    }

    /** A section for Event related declarations. */
    fun events(block: EventsBuilder<Change>.() -> Unit) {
        EventsBuilder(eventTransformers).also(block)
    }

    fun build(): Knot<State, Change, Action> = DefaultKnot(
        initialState = checkNotNull(initialState) { "knot { state { initial } } must be set" },
        observeOn = observeOn,
        reduceOn = reduceOn,
        reduce = checkNotNull(reduce) { "knot { state { reduce } } must be set" },
        eventTransformers = eventTransformers,
        actionTransformers = actionTransformers
    )

    @KnotDsl
    class StateBuilder<State : Any, Change : Any, Action : Any>
    internal constructor() {
        internal var reduce: Reduce<State, Change, Action>? = null

        /** Mandatory initial [State] of the [Knot]. */
        var initial: State? = null

        /** An optional [Scheduler] used for dispatching state changes. */
        var observeOn: Scheduler? = null

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
         *    reduce { change ->
         *       when (change) {
         *          is Change.Load -> copy(value = "loading") + Action.Load
         *          is Change.Load.Success -> copy(value = change.payload).only
         *          is Change.Load.Failure -> copy(value = "failed").only
         *       }
         *    }
         * ```
         */
        fun reduce(reduce: Reduce<State, Change, Action>) {
            this.reduce = reduce
        }

        /** Turns [State] into an [Effect] without [Action]. */
        val State.only: Effect<State, Action> get() = Effect(this)

        /** Combines [State] and [Action] into [Effect]. */
        operator fun State.plus(action: Action) = Effect(this, action)
    }

    @KnotDsl
    class ActionsBuilder<Change : Any, Action : Any>
    internal constructor(
        private val actionTransformers: MutableList<ActionTransformer<Action, Change>>
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
         *    perform<Action.Load> { action ->
         *       action
         *          .flatMapSingle<Payload> { api.load() }
         *          .map<Change> { Change.Load.Success(it) }
         *          .onErrorReturn { Change.Load.Failure(it) }
         *    }
         * ```
         */
        inline fun <reified A : Action> perform(noinline transformer: ActionTransformer<A, Change>) {
            performAny(TypedActionTransformer(A::class.java, transformer))
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
         *      transform {
         *          userLocationObserver
         *              .map { Change.UserLocationUpdated(it) }
         *      }
         * ```
         */
        fun transform(transformer: EventTransformer<Change>) {
            eventTransformers += transformer
        }
    }
}

/** A function accepting the *State* and a *Change* and returning a new *State*. */
typealias Reduce<State, Change, Action> = State.(change: Change) -> Effect<State, Action>

/** A function returning an [Observable] *Change*. */
typealias EventTransformer<Change> = () -> Observable<Change>

/** A function used for performing given *Action* and emitting resulting *Change* or *Changes*. */
typealias ActionTransformer<Action, Change> = (action: Observable<Action>) -> Observable<Change>

class TypedActionTransformer<Action : Any, Change : Any, A : Action>(
    private val type: Class<A>,
    private val transform: ActionTransformer<A, Change>
) : ActionTransformer<Action, Change> {
    override fun invoke(action: Observable<Action>): Observable<Change> {
        return transform(action.ofType(type))
    }
}
