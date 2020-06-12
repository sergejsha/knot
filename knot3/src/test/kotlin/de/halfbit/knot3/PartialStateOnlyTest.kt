package de.halfbit.knot3

import org.junit.Test

class PartialStateOnlyTest {

    private data class State(
        val data: List<String> = emptyList()
    )

    private sealed class Change {
        object Ignite : Change()
    }

    private interface PartialReducer {
        fun onStateChanged(partialState: State): State
    }

    private class PartialStateWithActionReducer(
        private val stateData: String? = null
    ) : PartialReducer {
        override fun onStateChanged(partialState: State): State {
            return stateData?.let {
                partialState.copy(data = partialState.data + it)
            } ?: partialState
        }
    }

    @Test
    fun `Partial state gets dispatched`() {
        val knot = createKnot(
            partialReducers = listOf(
                PartialStateWithActionReducer(stateData = "partial-1"),
                PartialStateWithActionReducer(stateData = "partial-2"),
                PartialStateWithActionReducer(stateData = "partial-3")
            )
        )

        val states = knot.state.test()
        knot.change.accept(Change.Ignite)

        states.assertValues(
            State(
                data = emptyList()
            ),
            State(
                data = listOf(
                    "partial-1",
                    "partial-2",
                    "partial-3"
                )
            )
        )
    }

    private fun createKnot(
        partialReducers: List<PartialReducer>
    ): CompositeKnot<State> = compositeKnot<State> {
        state { initial = State() }
    }.apply {
        registerDelegate<Change, Nothing> {
            changes {
                reduce<Change.Ignite> {
                    partialReducers.dispatchStateOnly(this) { reducer, partialState ->
                        reducer.onStateChanged(partialState)
                    }.only
                }
            }
        }
        compose()
    }
}
