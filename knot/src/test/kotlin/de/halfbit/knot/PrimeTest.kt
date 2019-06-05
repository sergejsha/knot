package de.halfbit.knot

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class PrimeTest {

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
    fun `CompositeKnot emits IllegalStateException if reducer cannot be found`() {
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

        knot.registerPrime<Change, Action> {
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

        knot.registerPrime<Change, Action> {
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
        knot.registerPrime<Change, Action> {
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
        knot.registerPrime<Change, Action> {
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

    @Test
    fun `Composed CompositeKnot subscribes event source`() {

        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }

        val eventSource = PublishSubject.create<Unit>()
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { only }
            }
            events {
                source {
                    eventSource.map { Change.A }
                }
            }
        }

        knot.compose()
        assertThat(eventSource.hasObservers()).isTrue()
    }

    @Test
    fun `Disposed CompositeKnot unsubscribes event source`() {

        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }

        val eventSource = PublishSubject.create<Unit>()
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { only }
            }
            events {
                source {
                    eventSource.map { Change.A }
                }
            }
        }

        knot.compose()
        knot.disposable.dispose()
        assertThat(eventSource.hasObservers()).isFalse()
    }

    @Test
    fun `Reducer throws error on unexpected()`() {
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { unexpected(it) }
            }
        }

        val observer = knot.state.test()
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertError(IllegalStateException::class.java)
    }

    @Test
    fun `Prime actions { watchAll } receives Action`() {
        val watcher = PublishSubject.create<Action>()
        val observer = watcher.test()
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                watchAll { watcher.onNext(it) }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Action.A
        )
    }

    @Test
    fun `Prime actions { watch } receives Action`() {
        val watcher = PublishSubject.create<Action>()
        val observer = watcher.test()
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                watch<Action.A> { watcher.onNext(it) }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Action.A
        )
    }

    @Test
    fun `Prime actions { intercept } receives Action`() {
        val watcher = PublishSubject.create<Action>()
        val observer = watcher.test()
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                intercept { action -> action.doOnNext { watcher.onNext(it) } }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Action.A
        )
    }

    @Test
    fun `Prime changes { watchAll } receives Change`() {
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
                watchAll { watcher.onNext(it) }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Change.A
        )
    }

    @Test
    fun `Prime changes { watch } receives Change`() {
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
                watch<Change.A> { watcher.onNext(it) }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Change.A
        )
    }

    @Test
    fun `Prime changes { intercept } receives Change`() {
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = testCompositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
                intercept { change -> change.doOnNext { watcher.onNext(it) } }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Change.A
        )
    }

    @Test
    fun `CompositeKnot state { watchAll } receives State`() {
        val watcher = PublishSubject.create<State>()
        val knot = testCompositeKnot<State> {
            state {
                initial = State("empty")
                watchAll { watcher.onNext(it) }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `CompositeKnot changes { watchAll } receives Change`() {
        val watcher = PublishSubject.create<Any>()
        val knot = testCompositeKnot<State> {
            state {
                initial = State("empty")
            }
            changes {
                watchAll { watcher.onNext(it) }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            Change.A
        )
    }

    @Test
    fun `CompositeKnot actions { watchAll } receives Action`() {
        val watcher = PublishSubject.create<Any>()
        val knot = testCompositeKnot<State> {
            state {
                initial = State("empty")
            }
            actions {
                watchAll { watcher.onNext(it) }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one") + Action.A }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            Action.A
        )
    }

    @Test
    fun `CompositeKnot state { watch } receives State`() {
        val watcher = PublishSubject.create<State>()
        val knot = testCompositeKnot<State> {
            state {
                initial = State("empty")
                watch<State> { watcher.onNext(it) }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `Prime state { watchAll } receives State`() {
        val watcher = PublishSubject.create<State>()
        val knot = testCompositeKnot<State> {
            state {
                initial = State("empty")
            }
        }
        knot.registerPrime<Change, Action> {
            state {
                watchAll { watcher.onNext(it) }
            }
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `Prime state { watch } receives State`() {
        val watcher = PublishSubject.create<State>()
        val knot = testCompositeKnot<State> {
            state {
                initial = State("empty")
            }
        }
        knot.registerPrime<Change, Action> {
            state {
                watch<State> { watcher.onNext(it) }
            }
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `Prime receives updates when uses state as events soruce`() {

        val knot = testCompositeKnot<State> {
            state {
                initial = State("empty")
            }
        }

        val stateObserver = knot.state.test()
        knot.registerPrime<Change, Unit> {
            changes {
                reduce<Change.A> {
                    State("one").only
                }
            }
            events {
                source {
                    knot.state
                        .filter { it.value == "empty" }
                        .map { Change.A }
                }
            }
        }
        knot.compose()

        stateObserver.assertValues(
            State("empty"),
            State("one")
        )
    }

}