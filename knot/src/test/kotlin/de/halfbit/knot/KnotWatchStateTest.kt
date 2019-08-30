package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import de.halfbit.knot.utils.SchedulerTester
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class KnotWatchStateTest {

    private sealed class State {
        object Zero : State()
        object One : State()
    }

    private sealed class Change {
        object One : Change()
    }

    private object Action

    @Test
    fun `state { watchAll } receives initial State`() {
        val watcher = PublishSubject.create<State>()
        val observer = watcher.test()
        knot<State, Change, Action> {
            state {
                initial = State.Zero
                watchAll { watcher.onNext(it) }
            }
            changes {
                reduce { this.only }
            }
        }
        observer.assertValues(State.Zero)
    }

    @Test
    fun `state { watchAll } receives State mutations`() {
        val watcher = PublishSubject.create<State>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State.Zero
                watchAll { watcher.onNext(it) }
            }
            changes {
                reduce { State.One.only }
            }
        }
        knot.state.test()
        knot.change.accept(Change.One)
        observer.assertValues(
            State.Zero,
            State.One
        )
    }

    @Test
    fun `state { watch } receives initial State`() {
        val watcher = PublishSubject.create<State>()
        val observer = watcher.test()
        knot<State, Change, Action> {
            state {
                initial = State.Zero
                watch<State.Zero> { watcher.onNext(it) }
            }
            changes {
                reduce { this.only }
            }
        }
        observer.assertValues(State.Zero)
    }

    @Test
    fun `state { watch } receives State mutations`() {
        val watcher = PublishSubject.create<State>()
        val observer = watcher.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State.Zero
                watch<State.One> { watcher.onNext(it) }
                watch<State.Zero> { watcher.onNext(it) }
            }
            changes {
                reduce { State.One.only }
            }
        }
        knot.state.test()
        knot.change.accept(Change.One)
        observer.assertValues(
            State.Zero,
            State.One
        )
    }

    @Test
    fun `state { watch } filters State mutations`() {
        val watcher = PublishSubject.create<State>()
        val observer = watcher.test()
        knot<State, Change, Action> {
            state {
                initial = State.Zero
                watch<State.One> { watcher.onNext(it) }
            }
            changes {
                reduce { this.only }
            }
        }
        observer.assertNoValues()
    }

    @Test
    fun `changes { watchOn } gets applied`() {
        val schedulerTester = SchedulerTester()
        knot<State, Change, Action> {
            state {
                initial = State.Zero
                watchOn = schedulerTester.scheduler("one")
                watchAll { }
            }
            changes {
                reduce { this.only }
            }
        }
        schedulerTester.assertSchedulers("one")
    }

    @Test
    fun `changes { watchOn } is null by default`() {
        knot<State, Change, Action> {
            state {
                initial = State.Zero
                assertThat(watchOn).isNull()
            }
            changes {
                reduce { this.only }
            }
        }
    }

    @Test
    fun `changes { watchOn } gets applied before each watcher`() {
        val schedulerTester = SchedulerTester()
        knot<State, Change, Action> {
            state {
                initial = State.Zero
                watchOn = schedulerTester.scheduler("one")
                watchAll { }
                watchOn = schedulerTester.scheduler("two")
                watchAll { }
            }
            changes {
                reduce { this.only }
            }
        }
        schedulerTester.assertSchedulers("one")
    }
}