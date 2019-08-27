package de.halfbit.knot

import com.google.common.truth.Truth
import io.reactivex.schedulers.Schedulers
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
        var visited = false
        val scheduler = Schedulers.from {
            visited = true
            it.run()
        }
        knot<State, Change, Action> {
            state {
                initial = State.Zero
                watchOn = scheduler
                watch<State.One> { }
            }
            changes {
                reduce { this.only }
            }
        }
        Truth.assertThat(visited).isTrue()
    }

    @Test(expected = IllegalStateException::class)
    fun `changes { watchOn } fails if declared after a watcher`() {
        knot<State, Change, Action> {
            state {
                initial = State.Zero
                watch<State.One> { }
                watchOn = Schedulers.from { }
            }
            changes {
                reduce { this.only }
            }
        }
    }
}