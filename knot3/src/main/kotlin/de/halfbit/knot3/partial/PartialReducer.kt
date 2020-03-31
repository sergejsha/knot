package de.halfbit.knot3.partial

import de.halfbit.knot3.Effect

/**
 * Partial reducer is useful when the same change (or its payload) needs to be
 * provided to  multiple reducers. Such reducers should then implement the
 * `PartialReducer` interface and the main reducer, which initially receives the
 * change, should dispatch it (or its payload) to all partial reducers. Each
 * partial reducer should return a new state back, which will then be provided
 * to the next reducer in the list and so on until all partial reducers are
 * processed. The resulting state can be returned back from the main reducer to
 * knot.
 *
 * Use [dispatch] extension function for dispatching the payload to partial
 * reducers.
 */
interface PartialReducer<State : Any, Payload : Any, Action : Any> {
    fun reduce(state: State, payload: Payload): Effect<State, Action>

    /** Turns [State] into an [Effect] without [Action]. */
    val State.only: Effect<State, Action> get() = Effect.WithAction(this)

    /** Combines [State] and [Action] into [Effect]. */
    operator fun State.plus(action: Action?) = Effect.WithAction(this, action)
}

/**
 * This extension function implements the contract between the main reducer
 * and list of partial reducers as defined by [PartialReducer].
 *
 * ```kotlin
 * val reducers: List<PartialReducer<State, Payload, Action>>
 *
 * knot<State, Change, Action> {
 *   state {
 *     initial = State.Initial
 *   }
 *   changes {
 *     reduce { change ->
 *       reducers.dispatch(this, change.payload)
 *     }
 *   }
 * }
 * ```
 */
fun <State : Any, Payload : Any, Action : Any> Collection<PartialReducer<State, Payload, Action>>.dispatch(
    state: State, payload: Payload
): Effect<State, Action> {
    val actions = mutableListOf<Action>()
    val newState = fold(state) { partialState, reducer ->
        when (val effect = reducer.reduce(partialState, payload)) {
            is Effect.WithAction -> {
                effect.action?.let { actions += it }
                effect.state
            }
            is Effect.WithActions -> {
                actions += effect.actions
                effect.state
            }
        }
    }
    return if (actions.isEmpty()) Effect.WithAction(newState)
    else Effect.WithActions(newState, actions)
}
