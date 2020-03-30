package de.halfbit.knot3.partial

/**
 * This is a light weight version of [PartialReducer] which should be used for
 * partial reducers which never emit actions.
 */
interface StateOnlyPartialReducer<State : Any, Payload : Any> {
    fun reduce(state: State, payload: Payload): State
}

/**
 * This extension function dispatches state and payload to all partial reducers
 * in the list in accordance to the rules described in [StateOnlyPartialReducer].
 */
fun <State : Any, Payload : Any> Collection<StateOnlyPartialReducer<State, Payload>>.dispatch(
    state: State, payload: Payload
): State = fold(state) { partialState, reducer -> reducer.reduce(partialState, payload) }
