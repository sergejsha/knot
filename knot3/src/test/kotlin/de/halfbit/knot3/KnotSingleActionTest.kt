package de.halfbit.knot3

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
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
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Load -> copy(value = "loading") + Action.Load
                        is Change.Load.Success -> copy(value = change.payload).only
                        is Change.Load.Failure -> copy(value = "failed").only
                    }
                }
            }
            actions {
                perform<Action.Load> {
                    flatMapSingle { Single.just("loaded") }
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
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Load -> copy(value = "loading") + Action.Load
                        is Change.Load.Success -> copy(value = change.payload).only
                        is Change.Load.Failure -> copy(value = "failed").only
                    }
                }
            }
            actions {
                perform<Action.Load> {
                    flatMapSingle<String> { Single.error(Exception()) }
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
    fun `Execute Action with many single Failure change`() {
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Load -> copy(value = "loading") + Action.Load
                        is Change.Load.Success -> copy(value = change.payload).only
                        is Change.Load.Failure -> copy(value = "failed").only
                    }
                }
            }
            actions {
                perform<Action.Load> {
                    flatMapSingle<String> { Single.error(Exception()) }
                        .map<Change> { Change.Load.Success(it) }
                        .onErrorReturn { Change.Load.Failure(it) }
                }
            }
        }

        val observer = knot.state.test()
        knot.change.accept(Change.Load)
        knot.change.accept(Change.Load)
        knot.change.accept(Change.Load)

        observer.assertValues(
            State("empty"),
            State("loading"),
            State("failed"),
            State("loading"),
            State("failed"),
            State("loading"),
            State("failed")
        )
    }

    @Test
    fun `Execute Action with multiple changes`() {
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Load -> copy(value = "loading") + Action.Load
                        is Change.Load.Success -> copy(value = change.payload).only
                        is Change.Load.Failure -> copy(value = "failed").only
                    }
                }
            }
            actions {
                perform<Action.Load> {
                    this
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
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Load -> copy(value = "loading") + Action.Load
                        else -> error("unexpected $change in $this")
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