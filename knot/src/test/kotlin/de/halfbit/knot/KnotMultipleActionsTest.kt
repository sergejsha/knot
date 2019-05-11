package de.halfbit.knot

import io.reactivex.Single
import org.junit.Test

class KnotMultipleActionsTest {

    private data class State(val value: String)

    private sealed class Action {
        object One : Action()
        object Two : Action()
        object Three : Action()
    }

    private sealed class Change {
        object Launch : Change()
        object OneDone : Change()
        object TwoDone : Change()
        object ThreeDone : Change()
    }

    @Test
    fun `Perform multiple actions in sequence`() {
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Launch -> copy(value = "zero") + Action.One
                        is Change.OneDone -> copy(value = "one") + Action.Two
                        is Change.TwoDone -> copy(value = "two") + Action.Three
                        is Change.ThreeDone -> copy(value = "three").only
                    }
                }
            }
            actions {
                perform<Action.One> { it.flatMapSingle { Single.just(Change.OneDone) } }
                perform<Action.Two> { it.flatMapSingle { Single.just(Change.TwoDone) } }
                perform<Action.Three> { it.flatMapSingle { Single.just(Change.ThreeDone) } }
            }
        }

        val observer = knot.state.test()
        knot.change.accept(Change.Launch)

        observer.assertValues(
            State("empty"),
            State("zero"),
            State("one"),
            State("two"),
            State("three")
        )
    }

}