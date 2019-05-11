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
        val knot = compositeKnot<State, Change, Action> {
            state { initial = State("empty") }
        }

        val observer = knot.state.test()
        knot.compose(Composition())
        knot.change.accept(Change.A)

        observer.assertError(IllegalStateException::class.java)
    }

    @Test
    fun `CompositeKnot picks reducers by change type`() {
        val composition = Composition<State, Change, Action>().apply {
            reducers[Change.A::class] = { change -> Effect(copy(value = change.value)) }
            reducers[Change.B::class] = { change -> Effect(copy(value = change.value)) }
        }

        val knot = compositeKnot<State, Change, Action> {
            state { initial = State("empty") }
        }

        val observer = knot.state.test()
        knot.compose(composition)

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

        val composition = Composition<State, Change, Action>().apply {
            eventTransformers += { changeA.map { Change.A } }
            eventTransformers += { changeB.map { Change.B } }

            reducers[Change.A::class] = { change -> Effect(copy(value = change.value)) }
            reducers[Change.B::class] = { change -> Effect(copy(value = change.value)) }
        }

        val knot = compositeKnot<State, Change, Action> {
            state { initial = State("empty") }
        }

        val observer = knot.state.test()
        knot.compose(composition)

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

        val composition = Composition<State, Change, Action>().apply {
            actionTransformers += { action -> action.filter { it is Action.A }.map { Change.ADone } }
            actionTransformers += { action -> action.filter { it is Action.B }.map { Change.BDone } }

            reducers[Change.A::class] = { change ->
                Effect(
                    copy(value = change.value),
                    Action.A
                )
            }
            reducers[Change.ADone::class] = { change ->
                Effect(
                    copy(
                        value = change.value
                    )
                )
            }
            reducers[Change.B::class] = { change ->
                Effect(
                    copy(value = change.value),
                    Action.B
                )
            }
            reducers[Change.BDone::class] = { change ->
                Effect(
                    copy(
                        value = change.value
                    )
                )
            }
        }

        val knot = compositeKnot<State, Change, Action> {
            state { initial = State("empty") }
        }

        val observer = knot.state.test()
        knot.compose(composition)

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