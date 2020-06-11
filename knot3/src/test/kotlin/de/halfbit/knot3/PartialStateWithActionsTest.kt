package de.halfbit.knot3

import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import org.junit.Test

class PartialStateWithActionsTest {

    private data class State(
        val data: List<String> = emptyList()
    )

    private sealed class Change {
        object Ignite : Change()
    }

    private sealed class Action {
        object One : Action()
        object Two : Action()
        object Three : Action()
    }

    private interface PartialReducer : Partial<State, Action> {
        fun onStateChanged(partialState: State): Effect<State, Action>
    }

    private class PartialStateWithActionReducer(
        private val stateData: String? = null,
        private val action: Action? = null
    ) : PartialReducer {
        override fun onStateChanged(partialState: State): Effect<State, Action> {
            val state = stateData?.let {
                partialState.copy(data = partialState.data + it)
            } ?: partialState
            return if (action == null) state.only else state + action
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

    @Test
    fun `Partial actions get dispatched`() {
        val actionsObserver = BehaviorSubject.create<Action>()
        val knot = createKnot(
            partialReducers = listOf(
                PartialStateWithActionReducer(action = Action.One),
                PartialStateWithActionReducer(action = Action.Two),
                PartialStateWithActionReducer(action = null),
                PartialStateWithActionReducer(action = Action.Three)
            ),
            actionsObserver = actionsObserver
        )

        knot.state.test()
        val actions = actionsObserver.test()
        knot.change.accept(Change.Ignite)

        actions.assertValues(
            Action.One,
            Action.Two,
            Action.Three
        )
    }

    @Test
    fun `Partial state and actions get dispatched`() {
        val actionsObserver = BehaviorSubject.create<Action>()
        val knot = createKnot(
            partialReducers = listOf(
                PartialStateWithActionReducer(stateData = "partial-1", action = Action.One),
                PartialStateWithActionReducer(stateData = null, action = Action.Two),
                PartialStateWithActionReducer(stateData = "partial-2", action = null),
                PartialStateWithActionReducer(stateData = "partial-3", action = Action.Three)
            ),
            actionsObserver = actionsObserver
        )

        val states = knot.state.test()
        val actions = actionsObserver.test()
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

        actions.assertValues(
            Action.One,
            Action.Two,
            Action.Three
        )
    }

    private fun createKnot(
        partialReducers: List<PartialReducer>,
        actionsObserver: Subject<Action>? = null
    ): CompositeKnot<State> = compositeKnot<State> {
        state { initial = State() }
    }.apply {
        registerDelegate<Change, Action> {
            changes {
                reduce<Change.Ignite> {
                    partialReducers.dispatch(this) { reducer, partialState ->
                        reducer.onStateChanged(partialState)
                    }
                }
            }
            actionsObserver?.let {
                actions {
                    watchAll { actionsObserver.onNext(it) }
                }
            }
        }
        compose()
    }
}
