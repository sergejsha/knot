package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test

class KnotSingleActionTest {

    private data class State(val value: String)

    private sealed class Action {
        object Load : Action()
    }

    private sealed class Change {
        object Load : Change() {
            data class Success(val payload: String) : Change()
            data class Failure(val error: Throwable) : Change()
        }
    }

    @Test
    fun `Execute Action with single Success change`() {
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
                reduce { change, state ->
                    when (change) {
                        is Change.Load -> state.copy(value = "loading") and Action.Load
                        is Change.Load.Success -> state.copy(value = change.payload).only()
                        is Change.Load.Failure -> state.copy(value = "failed").only()
                    }
                }
            }
            action {
                perform<Action.Load> { action ->
                    action
                        .flatMapSingle { Single.just("loaded") }
                        .map<Change> { Change.Load.Success(it) }
                        .onErrorReturn { Change.Load.Failure(it) }
                }
            }
        }

        val observer = knot.state.test()
        knot.change.accept(Change.Load)

        observer.assertValues(
            State("empty"),
            State("loading"),
            State("loaded")
        )
    }

    @Test
    fun `Execute Action with single Failure change`() {
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
                reduce { change, state ->
                    when (change) {
                        is Change.Load -> state.copy(value = "loading") and Action.Load
                        is Change.Load.Success -> state.copy(value = change.payload).only()
                        is Change.Load.Failure -> state.copy(value = "failed").only()
                    }
                }
            }
            action {
                perform<Action.Load> { action ->
                    action
                        .flatMapSingle<String> { Single.error(Exception()) }
                        .map<Change> { Change.Load.Success(it) }
                        .onErrorReturn { Change.Load.Failure(it) }
                }
            }
        }

        val observer = knot.state.test()
        knot.change.accept(Change.Load)

        observer.assertValues(
            State("empty"),
            State("loading"),
            State("failed")
        )
    }

    @Test
    fun `Execute Action with multiple changes`() {
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
                reduce { change, state ->
                    when (change) {
                        is Change.Load -> state.copy(value = "loading") and Action.Load
                        is Change.Load.Success -> state.copy(value = change.payload).only()
                        is Change.Load.Failure -> state.copy(value = "failed").only()
                    }
                }
            }
            action {
                perform<Action.Load> { action ->
                    action
                        .flatMap {
                            Observable
                                .fromArray("loading1", "loading2")
                                .concatWith(Single.error(Exception()))
                        }
                        .map<Change> { Change.Load.Success(it) }
                        .onErrorReturn { Change.Load.Failure(it) }
                }
            }
        }

        val observer = knot.state.test()
        knot.change.accept(Change.Load)

        observer.assertValues(
            State("empty"),
            State("loading"),
            State("loading1"),
            State("loading2"),
            State("failed")
        )
    }

    @Test
    fun `Action gets ignored if there is no handler`() {
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
                reduce { change, state ->
                    when (change) {
                        is Change.Load -> state.copy(value = "loading") and Action.Load
                        else -> error("unexpected $change in $state")
                    }
                }
            }
        }

        val observer = knot.state.test()
        knot.change.accept(Change.Load)

        observer.assertValues(
            State("empty"),
            State("loading")
        )
    }

}