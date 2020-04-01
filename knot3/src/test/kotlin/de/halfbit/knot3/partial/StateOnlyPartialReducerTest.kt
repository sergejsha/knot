package de.halfbit.knot3.partial

import de.halfbit.knot3.knot
import org.junit.Test

@ExperimentalStdlibApi
class StateOnlyPartialReducerTest {

    data class State(val value: String)
    data class Change(val value: String)
    data class Action(val value: String)

    @Test
    fun `partial reducers are processed in registration order`() {

        val reducers = buildList {
            add(
                object : StateOnlyPartialReducer<State, String> {
                    override fun reduce(state: State, payload: String) =
                        state.copy(value = "${state.value}, $payload reducer1")
                }
            )
            add(
                object : StateOnlyPartialReducer<State, String> {
                    override fun reduce(state: State, payload: String) =
                        state.copy(value = "${state.value}, $payload reducer2")
                }
            )
        }

        val knot =
            knot<State, Change, Action> {
                state {
                    initial = State("initial")
                }

                changes {
                    reduce { change ->
                        reducers.dispatch(this, change.value).only
                    }
                }
            }

        val states = knot.state.test()
        knot.change.accept(Change("one"))

        states.assertValues(
            State("initial"),
            State("initial, one reducer1, one reducer2")
        )
    }
}