package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import de.halfbit.knot.dsl.Reducer
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
    fun `Knot contains initial state`() {
        val state = State()
        knot = tieKnot {
            state { initial = state }
        }
        assertThat(knot.currentState).isEqualTo(state)
    }

    @Test
    fun `Knot updates current state`() {
        knot = tieKnot {
            state { initial = State() }
            on<Command> {
                updateState {
                    it.map<Reducer<State>> { reduce { state.copy(value = 2) } }
                }
            }
        }
        knot.command.accept(Command)
        assertThat(knot.currentState).isEqualTo(State(2))
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

    private data class State(val value: Int = 0)
    private object Command
}

