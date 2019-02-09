package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KnotDslTest {

    @Test(expected = IllegalStateException::class)
    fun `DSL requires initial state`() {
        knot<State, Command> { }
    }

    @Test
    fun `DSL builder creates Knot`() {
        val knot = knot<State, Command> {
            state { initial = State() }
        }
        assertThat(knot).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'state'`() {
        val knot = knot<State, Command> {
            state { initial = State() }
        }
        assertThat(knot.state).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'command'`() {
        val knot = knot<State, Command> {
            state { initial = State() }
        }
        assertThat(knot.command).isNotNull()
    }

    @Test
    fun `Knot dispatches initial state`() {
        val state = State()
        val knot = knot<State, Command> {
            state { initial = state }
        }
        val observer = knot.state.test()
        observer.assertValue(state)
    }

    private class State
    private class Command
}

