package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
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
    fun `Execute Action with many single Success change`() {
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
        knot.change.accept(Change.Load)
        knot.change.accept(Change.Load)

        observer.assertValues(
            State("empty"),
            State("loading"),
            State("loaded"),
            State("loading"),
            State("loaded"),
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
    fun `Execute Action with many single Failure changes`() {
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Load -> copy(value = "loading") + Action.Load
                        is Change.Load.Success -> unexpected(change)
                        is Change.Load.Failure -> copy(value = "failed").only
                    }
                }
            }
            actions {
                perform<Action.Load> {
                    flatMapSingle {
                        Single.error<String>(Exception())
                            .map<Change> { Change.Load.Success(it) }
                            .onErrorReturn { Change.Load.Failure(it) }
                    }
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
    fun `actions replace when switchMapSingle is used`() {
        val scheduler = TestScheduler()
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Load -> copy(value = "loading") + Action.Load
                        is Change.Load.Success -> copy(value = change.payload).only
                        is Change.Load.Failure -> unexpected(change)
                    }
                }
            }
            actions {
                perform<Action.Load> {
                    switchMapSingle {
                        Single.just("data")
                            .subscribeOn(scheduler)
                            .map<Change> { Change.Load.Success(it) }
                            .onErrorReturn { Change.Load.Failure(it) }
                    }
                }
            }
        }

        val observer = knot.state.test()
        knot.change.accept(Change.Load)
        knot.change.accept(Change.Load)
        knot.change.accept(Change.Load)
        scheduler.triggerActions()

        observer.assertValues(
            State("empty"),
            State("loading"),
            State("loading"),
            State("loading"),
            State("data")
        )
    }

    @Test
    fun `actions stack when flatMapSingle is used`() {
        val scheduler = TestScheduler()
        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Load -> copy(value = "loading") + Action.Load
                        is Change.Load.Success -> copy(value = change.payload).only
                        is Change.Load.Failure -> unexpected(change)
                    }
                }
            }
            actions {
                perform<Action.Load> {
                    flatMapSingle {
                        Single.just("data")
                            .subscribeOn(scheduler)
                            .map<Change> { Change.Load.Success(it) }
                            .onErrorReturn { Change.Load.Failure(it) }
                    }
                }
            }
        }

        val observer = knot.state.test()
        knot.change.accept(Change.Load)
        knot.change.accept(Change.Load)
        knot.change.accept(Change.Load)
        scheduler.triggerActions()

        observer.assertValues(
            State("empty"),
            State("loading"),
            State("loading"),
            State("loading"),
            State("data"),
            State("data"),
            State("data")
        )
    }

    @Test
    fun `actions crash when terminated`() {

        var exception: Throwable? = null
        RxJavaPlugins.setErrorHandler { exception = it }

        val knot = knot<State, Change, Action> {
            state {
                initial = State("empty")
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.Load -> copy(value = "loading") + Action.Load
                        is Change.Load.Success -> copy(value = change.payload).only
                        is Change.Load.Failure -> only
                    }
                }
            }
            actions {
                perform<Action.Load> {
                    flatMapSingle { Single.error<String>(Exception()) }
                        .map<Change> { Change.Load.Success(it) }
                        .onErrorReturn { Change.Load.Failure(it) }
                }
            }
        }

        knot.state.test()
        knot.change.accept(Change.Load)
        RxJavaPlugins.reset()

        assertThat(exception)
            .hasMessageThat()
            .contains("Action must never terminate")
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