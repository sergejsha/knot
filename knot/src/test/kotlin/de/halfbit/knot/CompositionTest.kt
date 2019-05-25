package de.halfbit.knot

import com.google.common.truth.Truth
import io.reactivex.schedulers.Schedulers
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

        knot.registerPrime<Change, Action>() {
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

        knot.registerPrime<Change, Action> {
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

        knot.registerPrime<Change, Action>() {
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

    @Test
    fun `changes { reduceOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from {
            visited = true
            it.run()
        }
        val knot = testCompositeKnot<State> {
            state {
                initial = State("empty")
            }
            changes {
                reduceOn = scheduler
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { only }
            }
        }

        knot.compose()
        knot.state.test()
        knot.change.accept(Change.A)

        Truth.assertThat(visited).isTrue()
    }

    @Test
    fun `Disposed CompositeKnot ignores emitted changes`() {

        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action>() {
            changes {
                reduce<Change.A> { copy(value = it.value).only }
            }
        }

        val observer = knot.state.test()
        knot.compose()
        knot.disposable.dispose()

        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty")
        )
    }

    @Test
    fun `Disposed CompositeKnot ignores emitted events`() {

        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }

        val eventSource = PublishSubject.create<Unit>()
        knot.registerPrime<Change, Action>() {
            changes {
                reduce<Change.A> { copy(value = it.value).only }
            }
            events {
                source {
                    eventSource.map { Change.A }
                }
            }
        }

        val observer = knot.state.test()
        knot.compose()
        knot.disposable.dispose()

        eventSource.onNext(Unit)

        observer.assertValues(
            State("empty")
        )
    }
}