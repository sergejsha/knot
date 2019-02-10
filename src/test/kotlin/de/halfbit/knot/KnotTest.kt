package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class KnotTest {

    private lateinit var knot: Knot<State, Command>

    @Test(expected = IllegalStateException::class)
    fun `DSL requires initial state`() {
        knot = tieKnot { }
    }

    @Test
    fun `DSL builder creates Knot`() {
        knot = tieKnot {
            state { initial = State() }
        }
        assertThat(knot).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'state'`() {
        knot = tieKnot {
            state { initial = State() }
        }
        assertThat(knot.state).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'command'`() {
        knot = tieKnot {
            state { initial = State() }
        }
        assertThat(knot.command).isNotNull()
    }

    @Test
    fun `Knot dispatches initial state`() {
        val state = State()
        knot = tieKnot {
            state { initial = state }
        }
        val observer = knot.state.test()
        observer.assertValue(state)
    }

    @Test
    fun `Knot subscribes to events`() {
        val event = PublishSubject.create<Unit>()

        val state = State()
        knot = tieKnot {
            state { initial = state }
            on(event) {
                issueCommand { it.map { Command } }
            }
        }

        assertThat(event.hasObservers()).isTrue()
    }

    @Test
    fun `Knot disposes events subscriptions`() {
        val event = PublishSubject.create<Unit>()

        val state = State()
        knot = tieKnot {
            state { initial = state }
            on(event) {
                issueCommand { it.map { Command } }
            }
        }
        knot.dispose()

        assertThat(event.hasObservers()).isFalse()
    }

    private class State
    private object Command
}

