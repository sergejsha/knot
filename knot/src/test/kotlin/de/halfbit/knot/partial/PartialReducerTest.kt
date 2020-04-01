package de.halfbit.knot.partial

import com.google.common.truth.Truth.assertThat
import de.halfbit.knot.Effect
import de.halfbit.knot.knot
import org.junit.Test

@ExperimentalStdlibApi
class PartialReducerTest {

    data class State(val value: String)
    data class Change(val value: String)
    data class Action(val value: String)

    @Test
    fun `partial reducers emit no actions`() {

        val actions = mutableListOf<Action>()
        val reducers = buildList {
            add(
                object : PartialReducer<State, String, Action> {
                    override fun reduce(state: State, payload: String): Effect<State, Action> {
                        return state.only
                    }
                }
            )
            add(
                object : PartialReducer<State, String, Action> {
                    override fun reduce(state: State, payload: String): Effect<State, Action> {
                        return state.only
                    }
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
                        reducers.dispatch(this, change.value)
                    }
                }

                actions {
                    watchAll { actions.add(it) }
                }
            }

        knot.change.accept(Change("one"))
        assertThat(actions).isEmpty()
    }

    @Test
    fun `partial reducers emit actions`() {

        val actions = mutableListOf<Action>()
        val reducers = buildList {
            add(
                object : PartialReducer<State, String, Action> {
                    override fun reduce(state: State, payload: String): Effect<State, Action> {
                        return state + Action("one")
                    }
                }
            )
            add(
                object : PartialReducer<State, String, Action> {
                    override fun reduce(state: State, payload: String): Effect<State, Action> {
                        return state + Action("two") + Action("three")
                    }
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
                        reducers.dispatch(this, change.value)
                    }
                }

                actions {
                    watchAll { actions.add(it) }
                }
            }

        knot.change.accept(Change("one"))
        assertThat(actions).containsExactly(
            Action("one"),
            Action("two"),
            Action("three")
        )
    }

    @Test
    fun `partial reducers are processed in registration order`() {

        val reducers = buildList {
            add(
                object : PartialReducer<State, String, Action> {
                    override fun reduce(state: State, payload: String): Effect<State, Action> {
                        return state.copy(value = "${state.value}, $payload reducer1").only
                    }
                }
            )
            add(
                object : PartialReducer<State, String, Action> {
                    override fun reduce(state: State, payload: String): Effect<State, Action> {
                        return state.copy(value = "${state.value}, $payload reducer2").only
                    }
                }
            )
            add(
                object : PartialReducer<State, String, Action> {
                    override fun reduce(state: State, payload: String): Effect<State, Action> {
                        return state.copy(value = "${state.value}, $payload reducer3").only
                    }
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
                        reducers.dispatch(this, change.value)
                    }
                }
            }

        val states = knot.state.test()
        knot.change.accept(Change("one"))

        states.assertValues(
            State("initial"),
            State("initial, one reducer1, one reducer2, one reducer3")
        )
    }
}