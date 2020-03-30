package de.halfbit.knot3

/**
 * Partial reducer is useful when the same change (or its payload data) needs to
 * be reduced by multiple reducers. The reducers should then implement `PartialReducer`
 * interface and the main reducer, which receives the change, should dispatch it
 * (or its payload data) to all partial reducers. Each partial reducer should return
 * a new state back, which will be then provided to the next reducer in the list and
 * so on, until all partial reducers are processed. The resulting state should be
 * returned back from the main reducer.
 *
 * ```kotlin
 * val reducers: List<PartialReducer<State, Playback, Action>>
 *
 * knot<State, Change, Action> {
 *   state {
 *     initial = State("initial")
 *   }
 *   changes {
 *     reduce { change ->
 *       reducers.reduce(this, change.payload)
 *     }
 *   }
 * }
 * ```
 *
 * For example, when a media player dispatches playback data and it needs to be
 * distributed to multiple primes, then the primes should also implement `PartialReducer`
 * interfaces and the main prime responsible for receiving playback data should provide
 * it to all those partial reducers.
 */
interface PartialReducer<State : Any, Payload : Any, Action : Any> {
    fun reduce(state: State, payload: Payload): Effect<State, Action>

    /** Turns [State] into an [Effect] without [Action]. */
    val State.only: Effect<State, Action> get() = Effect.WithAction(this)

    /** Combines [State] and [Action] into [Effect]. */
    operator fun State.plus(action: Action?) = Effect.WithAction(this, action)
}

/**
 * This is a utility function iterating though the partial reducers in the list and
 * providing them with state and payload data. The state returned by the previous
 * partial reducer is provides to the next partial reducer. Resulting state can be
 * returned back from the main reducers.
 */
fun <State : Any, Payload : Any, Action : Any> Collection<PartialReducer<State, Payload, Action>>.dispatch(
    state: State, payload: Payload
): Effect<State, Action> {
    val actions = mutableListOf<Action>()
    var newState = state
    for (reducer in this) {
        when (val effect = reducer.reduce(newState, payload)) {
            is Effect.WithAction -> {
                newState = effect.state
                effect.action?.let { actions += it }
            }
            is Effect.WithActions -> {
                newState = effect.state
                actions += effect.actions
            }
        }
    }
    return if (actions.isEmpty()) Effect.WithAction(newState) else Effect.WithActions(newState, actions)
}
