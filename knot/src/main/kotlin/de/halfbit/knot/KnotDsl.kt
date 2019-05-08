package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler

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

    fun actions(block: ActionsBuilder<Change, Action>.() -> Unit) {
        ActionsBuilder(actionTransformers).also(block)
    }

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

        var initial: State? = null
        var observeOn: Scheduler? = null
        var reduceOn: Scheduler? = null

        fun reduce(reduce: Reduce<State, Change, Action>) {
            this.reduce = reduce
        }

        fun State.only(): Effect<State, Action> = Effect(this)
        infix fun State.and(action: Action) = Effect(this, action)
    }

    @KnotDsl
    class ActionsBuilder<Change : Any, Action : Any>
    internal constructor(
        private val actionTransformers: MutableList<ActionTransformer<Action, Change>>
    ) {
        fun performAny(transformer: ActionTransformer<Action, Change>) {
            actionTransformers += transformer
        }

        inline fun <reified A : Action> perform(noinline transformer: ActionTransformer<A, Change>) {
            performAny(TypedActionTransformer(A::class.java, transformer))
        }
    }

    @KnotDsl
    class EventsBuilder<Change : Any>
    internal constructor(
        private val eventTransformers: MutableList<EventTransformer<Change>>
    ) {
        fun transform(transformer: EventTransformer<Change>) {
            eventTransformers += transformer
        }
    }
}

typealias Reduce<State, Change, Action> = (state: State, change: Change) -> Effect<State, Action>
typealias EventTransformer<Change> = () -> Observable<Change>
typealias ActionTransformer<Action, Change> = (action: Observable<Action>) -> Observable<Change>

class TypedActionTransformer<Action : Any, Change : Any, A : Action>(
    private val type: Class<A>,
    private val transform: ActionTransformer<A, Change>
) : ActionTransformer<Action, Change> {

    override fun invoke(action: Observable<Action>): Observable<Change> {
        return transform(action.ofType(type))
    }
}
