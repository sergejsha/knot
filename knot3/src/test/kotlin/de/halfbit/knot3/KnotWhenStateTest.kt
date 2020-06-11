package de.halfbit.knot3

import de.halfbit.knot3.utils.RxPluginsException
import org.junit.Rule
import org.junit.Test

class KnotWhenStateTest {

    @Rule
    @JvmField
    var rxPluginsException: RxPluginsException = RxPluginsException.none()

    private sealed class State {
        object Empty : State()
        object Loading : State()
        data class Content(val data: String) : State()
    }

    private sealed class Change {
        object Load : Change() {
            data class Success(val data: String) : Change()
        }
    }

    private sealed class Action {
        object Load : Action()
    }

    @Test
    fun `whenState lets known change through`() {
        val knot = createKnot(initialState = State.Empty)
        val states = knot.state.test()
        knot.change.accept(Change.Load)
        states.assertValues(
            State.Empty,
            State.Loading,
            State.Content("ok")
        )
    }

    @Test
    fun `whenState ignores unknown change through`() {
        val knot = createKnot(initialState = State.Loading)
        val states = knot.state.test()
        knot.change.accept(Change.Load)
        states.assertValues(State.Loading)
    }

    private fun createKnot(initialState: State): Knot<State, Change> =
        knot<State, Change, Action> {
            state { initial = initialState }
            changes {
                reduce { change ->
                    when (change) {
                        Change.Load -> whenState<State.Empty> {
                            State.Loading + Action.Load
                        }
                        is Change.Load.Success -> State.Content(change.data).only
                    }
                }
            }
            actions {
                perform<Action.Load> {
                    map { Change.Load.Success("ok") }
                }
            }
        }
}
