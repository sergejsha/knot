package de.halfbit.knot3.partial

/**
 * This is a lightweight version of the [PartialReducer] to be used with
 * partial reducers which never emit actions.
 *
 * Use [dispatch] extension function for dispatching the payload to partial
 * reducers.
 */
@Deprecated("Use free style implementation instead of this interface.")
interface StateOnlyPartialReducer<State : Any, Payload : Any> {
    fun reduce(state: State, payload: Payload): State
}

/**
 * This extension function implements the contract between the main reducer
 * and list of partial reducers as defined by [StateOnlyPartialReducer].
 *
 * ```kotlin
 * val reducers: List<StateOnlyPartialReducer<State, Payload>>
 *
 * knot<State, Change, Action> {
 *   state {
 *     initial = State.Initial
 *   }
 *   changes {
 *     reduce { change ->
 *       reducers.dispatch(this, change.payload).only
 *     }
 *   }
 * }
 * ```
 */
@Deprecated(
    "Use Collection<Reducer>.dispatchStateOnly(state, block) with new [de.halfbit.knot3.Partial].",
    ReplaceWith("dispatchStateOnly(state, block)"),
    level = DeprecationLevel.WARNING
)
fun <State : Any, Payload : Any> Collection<StateOnlyPartialReducer<State, Payload>>.dispatch(
    state: State, payload: Payload
): State = fold(state) { partialState, reducer -> reducer.reduce(partialState, payload) }
