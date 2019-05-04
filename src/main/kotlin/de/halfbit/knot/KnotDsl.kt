package de.halfbit.knot

import io.reactivex.Maybe
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
    private var reducer: Reducer<State, Change, Action>? = null
    private var observeOn: Scheduler? = null
    private var reduceOn: Scheduler? = null
    private val eventTransformers = mutableListOf<EventTransformer<Change>>()
    private val actionTransformers = mutableListOf<ActionTransformer<Action, Change>>()

    fun state(block: OnState<State, Change, Action>.() -> Unit) {
        OnState<State, Change, Action>()
            .also {
                block(it)
                initialState = it.initial
                reducer = it.reducer
                observeOn = it.observeOn
                reduceOn = it.reduceOn
            }
    }

    fun onEvent(transformer: EventTransformer<Change>) {
        eventTransformers += transformer
    }

    fun onAnyAction(transformer: ActionTransformer<Action, Change>) {
        actionTransformers += transformer
    }

    inline fun <reified A : Action> onAction(noinline transformer: ActionTransformer<A, Change>) {
        onAnyAction(TypedActionTransformer(A::class.java, transformer))
    }

    fun build(): Knot<State, Change, Action> = DefaultKnot(
        initialState = checkNotNull(initialState) { "knot { state { initialState } } must be set" },
        reducer = checkNotNull(reducer) { "knot { state { reducer } } must be set" },
        observeOn = observeOn,
        reduceOn = reduceOn,
        eventTransformers = eventTransformers,
        actionTransformers = actionTransformers
    )
}

@KnotDsl
class OnState<State : Any, Change : Any, Action : Any>
internal constructor() {
    internal var reducer: Reducer<State, Change, Action>? = null

    var initial: State? = null
    var observeOn: Scheduler? = null
    var reduceOn: Scheduler? = null

    fun reduce(reducer: Reducer<State, Change, Action>) {
        this.reducer = reducer
    }
}

typealias Reducer<State, Change, Action> = (change: Change, state: State) -> Effect<State, Action>
typealias EventTransformer<Change> = () -> Observable<Change>
typealias ActionTransformer<Action, Change> = (action: Observable<Action>) -> Maybe<Change>

class TypedActionTransformer<Action : Any, Change : Any, A : Action>(
    private val type: Class<A>,
    private val transform: ActionTransformer<A, Change>
) : ActionTransformer<Action, Change> {

    override fun invoke(action: Observable<Action>): Maybe<Change> {
        return transform(action.ofType(type))
    }
}