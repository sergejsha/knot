package de.halfbit.knot

import com.google.common.truth.Truth
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class TestCompositeKnotTest {

    data class State(val value: String)
    data class Action(val value: String)
    data class Change(val value: String)

    @Test(expected = IllegalStateException::class)
    fun `TestCompositeKnot must be composed before emitting actions`() {
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.actionConsumer.accept(Action("one"))
    }

    @Test
    fun `TestCompositeKnot can emmit action`() {
        val watcher = PublishSubject.create<Any>()
        val observer = watcher.test()
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
            actions { watchAll { watcher.onNext(it) } }
        }
        knot.compose()

        with(knot.actionConsumer) {
            accept(Action("one"))
            accept(Action("two"))
        }

        observer.assertValues(
            Action("one"),
            Action("two")
        )
    }

    @Test
    fun `TestCompositeKnot can observe action`() {
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.compose()

        val observer = knot.action.test()
        knot.actionConsumer.accept(Action("one"))
        knot.actionConsumer.accept(Action("two"))

        observer.assertValues(
            Action("one"),
            Action("two")
        )
    }

    @Test
    fun `TestCompositeKnot can register prime`() {
        val watcher = PublishSubject.create<Any>()
        val observer = watcher.test()
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Any, Any> {
            state { watchAll { watcher.onNext(it) } }
        }
        knot.compose()
        observer.assertValues(State("empty"))
    }

    @Test
    fun `TestCompositeKnot can emmit change`() {
        val watcher = PublishSubject.create<Any>()
        val observer = watcher.test()
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
            changes { watchAll { watcher.onNext(it) } }
        }
        knot.registerPrime<Any, Any> {
            changes {
                reduce<Change> { only }
            }
        }
        knot.compose()

        with(knot.change) {
            accept(Change("one"))
            accept(Change("two"))
        }

        observer.assertValues(
            Change("one"),
            Change("two")
        )
    }

    @Test
    fun `isDisposed returns false if not has not been disposed`() {
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Unit> {
            changes { reduce<Change> { only } }
        }
        Truth.assertThat(knot.isDisposed).isFalse()
    }

    @Test
    fun `isDisposed returns true if Knot has been disposed`() {
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Unit> {
            changes { reduce<Change> { only } }
        }
        knot.compose()
        knot.dispose()
        Truth.assertThat(knot.isDisposed).isTrue()
    }

    @Test
    fun `Disposed Knot disposes events`() {
        val events = PublishSubject.create<Unit>()
        var isDisposed = false
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes { reduce<Change> { only } }
            events {
                source {
                    events
                        .doOnDispose { isDisposed = true }
                        .map { Change("change") }
                }
            }
        }
        knot.compose()
        knot.dispose()
        Truth.assertThat(isDisposed).isTrue()
    }

    @Test
    fun `Disposed Knot disposes actions`() {
        val scheduler = TestScheduler()
        val actions = PublishSubject.create<Unit>()
        var isDisposed = false
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { this + Action("do it") }
            }
            actions {
                perform<Action> {
                    actions
                        .subscribeOn(scheduler)
                        .doOnDispose { isDisposed = true }
                        .map { Change("change") }
                }
            }
        }
        knot.compose()
        knot.change.accept(Change("action"))
        knot.dispose()
        Truth.assertThat(isDisposed).isTrue()
    }
}