package de.halfbit.knot3

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PartialReducerTest {

    data class State(val value: String)
    data class Change(val value: String)
    data class Action(val value: String)

    @Test
    fun `partial reducers are processed in registration order`() {

        val actions = mutableListOf<Action>()

        val reducer1 = object : PartialReducer<State, String, Action> {
            override fun reduce(state: State, payload: String): Effect<State, Action> {
                return state.copy(value = "${state.value}, $payload reducer1") + Action("reducer1")
            }
        }

        val reducer2 = object : PartialReducer<State, String, Action> {
            override fun reduce(state: State, payload: String): Effect<State, Action> {
                return state.copy(value = "${state.value}, $payload reducer2") +
                        Action("reducer2.1") + Action("reducer2.2")
            }
        }

        val reducer3 = object : PartialReducer<State, String, Action> {
            override fun reduce(state: State, payload: String): Effect<State, Action> {
                return state.copy(value = "${state.value}, $payload reducer3").only
            }
        }

        val reducers = mutableListOf<PartialReducer<State, String, Action>>()
            .apply {
                this += reducer1
                this += reducer2
                this += reducer3
            }

        val knot = knot<State, Change, Action> {
            state {
                initial = State("initial")
            }

            changes {
                reduce { change ->
                    reducers.dispatch(this, change.value)
                }
            }

            actions {
                watchAll { actions.add(it) }
            }

        }

        val states = knot.state.test()
        knot.change.accept(Change("one"))

        states.assertValues(
            State("initial"),
            State("initial, one reducer1, one reducer2, one reducer3")
        )
        assertThat(actions).containsExactly(
            Action("reducer1"),
            Action("reducer2.1"),
            Action("reducer2.2")
        )
    }
}