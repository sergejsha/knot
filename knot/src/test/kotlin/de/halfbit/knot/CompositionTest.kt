package de.halfbit.knot

import io.reactivex.subjects.PublishSubject
import org.junit.Test

class CompositionTest {

    private data class State(val value: String)

    private sealed class Change(val value: String) {
        object A : Change("a")
        object ADone : Change("a-done")
        object B : Change("b")
        object BDone : Change("b-done")
    }

    private sealed class Action {
        object A : Action()
        object B : Action()
    }

    @Test
    fun `CompositeKnot terminates with IllegalStateException if reducer cannot be found`() {
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }

        val observer = knot.state.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertError(IllegalStateException::class.java)
    }

    @Test
    fun `CompositeKnot picks reducers by change type`() {

        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.getComposition<Change, Action>().registerPrime {
            changes {
                reduce<Change.A> { copy(value = it.value).only }
                reduce<Change.B> { copy(value = it.value).only }
            }
        }

        val observer = knot.state.test()
        knot.compose()

        knot.change.accept(Change.A)
        knot.change.accept(Change.B)

        observer.assertValues(
            State("empty"),
            State("a"),
            State("b")
        )
    }

    @Test
    fun `CompositeKnot transforms events into changes`() {
        val changeA = PublishSubject.create<Unit>()
        val changeB = PublishSubject.create<Unit>()

        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.getComposition<Change, Action>().registerPrime {
            changes {
                reduce<Change.A> { copy(value = it.value).only }
                reduce<Change.B> { copy(value = it.value).only }
            }
            events {
                source { changeA.map { Change.A } }
                source { changeB.map { Change.B } }
            }
        }

        val observer = knot.state.test()
        knot.compose()

        changeA.onNext(Unit)
        changeB.onNext(Unit)

        observer.assertValues(
            State("empty"),
            State("a"),
            State("b")
        )
    }

    @Test
    fun `CompositeKnot performs actions`() {
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.getComposition<Change, Action>().registerPrime {
            changes {
                reduce<Change.A> { copy(value = it.value) + Action.A }
                reduce<Change.ADone> { copy(value = it.value).only }
                reduce<Change.B> { copy(value = it.value) + Action.B }
                reduce<Change.BDone> { copy(value = it.value).only }
            }
            actions {
                perform<Action.A> { map { Change.ADone } }
                perform<Action.B> { map { Change.BDone } }
            }
        }

        val observer = knot.state.test()
        knot.compose()

        knot.change.accept(Change.A)
        knot.change.accept(Change.B)

        observer.assertValues(
            State("empty"),
            State("a"),
            State("a-done"),
            State("b"),
            State("b-done")
        )
    }

}