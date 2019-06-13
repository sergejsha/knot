package de.halfbit.knot

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
}