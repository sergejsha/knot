package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CompositeKnotColdEventsTest {

    private data class State(val value: String)
    private sealed class Change {
        object A : Change()
        object B : Change()
    }

    private interface Action

    @Test
    fun `coldEvents not subscribed if no state observers are registered`() {

        val changeASubscribed = AtomicInteger()
        val changeA = PublishSubject.create<Unit>()
            .doOnSubscribe { changeASubscribed.incrementAndGet() }
            .doFinally { changeASubscribed.decrementAndGet() }

        val changeBSubscribed = AtomicInteger()
        val changeB = PublishSubject.create<Unit>()
            .doOnSubscribe { changeBSubscribed.incrementAndGet() }
            .doFinally { changeBSubscribed.decrementAndGet() }

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeA.map { Change.A } }
            }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeB.map { Change.B } }
            }
        }

        knot.compose()

        assertThat(changeASubscribed.get()).isEqualTo(0)
        assertThat(changeBSubscribed.get()).isEqualTo(0)
    }

    @Test
    fun `coldEvents subscribed after first state observer is registered`() {

        val changeASubscribed = AtomicInteger()
        val changeA = PublishSubject.create<Unit>()
            .doOnSubscribe { changeASubscribed.incrementAndGet() }
            .doFinally { changeASubscribed.decrementAndGet() }

        val changeBSubscribed = AtomicInteger()
        val changeB = PublishSubject.create<Unit>()
            .doOnSubscribe { changeBSubscribed.incrementAndGet() }
            .doFinally { changeBSubscribed.decrementAndGet() }

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeA.map { Change.A } }
            }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeB.map { Change.B } }
            }
        }

        knot.compose()
        knot.state.subscribe { }

        assertThat(changeASubscribed.get()).isEqualTo(1)
        assertThat(changeBSubscribed.get()).isEqualTo(1)
    }

    @Test
    fun `coldEvents stays subscribed after second state observer is registered`() {

        val changeASubscribed = AtomicInteger()
        val changeA = PublishSubject.create<Unit>()
            .doOnSubscribe { changeASubscribed.incrementAndGet() }
            .doFinally { changeASubscribed.decrementAndGet() }

        val changeBSubscribed = AtomicInteger()
        val changeB = PublishSubject.create<Unit>()
            .doOnSubscribe { changeBSubscribed.incrementAndGet() }
            .doFinally { changeBSubscribed.decrementAndGet() }

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeA.map { Change.A } }
            }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeB.map { Change.B } }
            }
        }

        knot.compose()
        knot.state.subscribe { }
        knot.state.subscribe { }

        assertThat(changeASubscribed.get()).isEqualTo(1)
        assertThat(changeBSubscribed.get()).isEqualTo(1)
    }

    @Test
    fun `coldEvents stays subscribed after second state observer is unregistered`() {

        val changeASubscribed = AtomicInteger()
        val changeA = PublishSubject.create<Unit>()
            .doOnSubscribe { changeASubscribed.incrementAndGet() }
            .doFinally { changeASubscribed.decrementAndGet() }

        val changeBSubscribed = AtomicInteger()
        val changeB = PublishSubject.create<Unit>()
            .doOnSubscribe { changeBSubscribed.incrementAndGet() }
            .doFinally { changeBSubscribed.decrementAndGet() }

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeA.map { Change.A } }
            }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeB.map { Change.B } }
            }
        }

        knot.compose()
        knot.state.subscribe { }
        val second = knot.state.subscribe { }
        second.dispose()

        assertThat(changeASubscribed.get()).isEqualTo(1)
        assertThat(changeBSubscribed.get()).isEqualTo(1)
    }

    @Test
    fun `coldEvents gets unsubscribed after last state observer is unregistered`() {

        val changeASubscribed = AtomicInteger()
        val changeA = PublishSubject.create<Unit>()
            .doOnSubscribe { changeASubscribed.incrementAndGet() }
            .doFinally { changeASubscribed.decrementAndGet() }

        val changeBSubscribed = AtomicInteger()
        val changeB = PublishSubject.create<Unit>()
            .doOnSubscribe { changeBSubscribed.incrementAndGet() }
            .doFinally { changeBSubscribed.decrementAndGet() }

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeA.map { Change.A } }
            }
        }

        knot.registerPrime<Change, Action> {
            events {
                coldSource { changeB.map { Change.B } }
            }
        }

        knot.compose()

        val first = knot.state.subscribe { }
        val second = knot.state.subscribe { }
        assertThat(changeASubscribed.get()).isEqualTo(1)
        assertThat(changeBSubscribed.get()).isEqualTo(1)

        first.dispose()
        assertThat(changeASubscribed.get()).isEqualTo(1)
        assertThat(changeBSubscribed.get()).isEqualTo(1)

        second.dispose()
        assertThat(changeASubscribed.get()).isEqualTo(0)
        assertThat(changeBSubscribed.get()).isEqualTo(0)
    }
}
