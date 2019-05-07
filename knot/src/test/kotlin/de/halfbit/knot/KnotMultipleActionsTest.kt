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
                reduce { state, change ->
                    when (change) {
                        is Change.Launch -> state.copy(value = "zero") and Action.One
                        is Change.OneDone -> state.copy(value = "one") and Action.Two
                        is Change.TwoDone -> state.copy(value = "two") and Action.Three
                        is Change.ThreeDone -> state.copy(value = "three").only()
                    }
                }
            }
            action {
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