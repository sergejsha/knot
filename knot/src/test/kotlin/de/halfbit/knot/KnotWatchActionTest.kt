package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class KnotWatchActionTest {

    private object State
    private sealed class Change {
        object PerformAction : Change()
        object Done : Change()
    }

    private sealed class Action {
        object One : Action()
        object Two : Action()
    }

    @Test
    fun `actions { watchAll } receives Action with performers`() {
        val watcher = PublishSubject.create<Action>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { change ->
                    when (change) {
                        Change.PerformAction -> this + Action.One
                        Change.Done -> this.only
                    }
                }
            }
            actions {
                perform<Action.One> { map { Change.Done } }
                perform<Action.One> { map { Change.Done } }
                watchAll { watcher.onNext(it) }
            }
        }
        knot.change.accept(Change.PerformAction)
        observer.assertValues(Action.One)
    }

    @Test
    fun `actions { watchAll } receives Action without performers`() {
        val watchAll = PublishSubject.create<Action>()
        val observer = watchAll.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { change ->
                    when (change) {
                        Change.PerformAction -> this + Action.One
                        Change.Done -> this.only
                    }
                }
            }
            actions {
                watchAll { watchAll.onNext(it) }
            }
        }
        knot.change.accept(Change.PerformAction)
        observer.assertValues(Action.One)
    }

    @Test
    fun `actions { watch } receives Action`() {
        val watcher = PublishSubject.create<Action>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { change ->
                    when (change) {
                        Change.PerformAction -> this + Action.One
                        Change.Done -> this.only
                    }
                }
            }
            actions {
                watch<Action.One> { watcher.onNext(it) }
            }
        }
        knot.change.accept(Change.PerformAction)
        observer.assertValues(Action.One)
    }

    @Test
    fun `actions { watch } filters Action`() {
        val watcher = PublishSubject.create<Action>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { change ->
                    when (change) {
                        Change.PerformAction -> this + Action.One
                        Change.Done -> this.only
                    }
                }
            }
            actions {
                watch<Action.Two> { watcher.onNext(it) }
            }
        }
        knot.change.accept(Change.PerformAction)
        observer.assertNoValues()
    }

    @Test
    fun `actions { watchOn } is empty by default`() {
        knot<State, Change, Action> {
            state { initial = State }
            changes {
                reduce { change ->
                    when (change) {
                        Change.PerformAction -> this + Action.One
                        Change.Done -> this.only
                    }
                }
            }
            actions {
                assertThat(watchOn).isNull()
                watch<Action.One> { }
            }
        }
    }

    @Test
    fun `actions { watchOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from {
            visited = true
            it.run()
        }

        val knot = knot<State, Change, Action> {
            state { initial = State }
            changes {
                reduce { change ->
                    when (change) {
                        Change.PerformAction -> this + Action.One
                        Change.Done -> this.only
                    }
                }
            }
            actions {
                watchOn = scheduler
                watch<Action.One> { }
            }
        }

        knot.change.accept(Change.PerformAction)
        assertThat(visited).isTrue()
    }

    @Test(expected = IllegalStateException::class)
    fun `actions { watchOn } fails if declared after a watcher`() {
        knot<State, Change, Action> {
            state { initial = State }
            changes {
                reduce { change ->
                    when (change) {
                        Change.PerformAction -> this + Action.One
                        Change.Done -> this.only
                    }
                }
            }
            actions {
                watch<Action.One> { }
                watchOn = Schedulers.from { }
            }
        }
    }
}