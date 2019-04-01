package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Observable
import org.junit.Test

class KnotTest {

    private data class State(val value: Int = 0)
    private object Command
    private object Change

    private lateinit var knot: Knot<State, Command, Change>

    @Test(expected = IllegalStateException::class)
    fun `DSL requires initial state`() {
        knot = knot { }
    }

    @Test(expected = IllegalStateException::class)
    fun `DSL requires reducer`() {
        knot = knot {
            state {
                initial = State()
            }
        }
    }

    @Test
    fun `DSL builder creates Knot`() {
        knot = knot {
            state {
                initial = State()
                reduce { _, state -> effect(state) }
            }
        }
        assertThat(knot).isNotNull()
    }

    @Test
    fun `'state' is not null`() {
        knot = knot {
            state {
                initial = State()
                reduce { _, state -> effect(state) }
            }
        }
        assertThat(knot.state).isNotNull()
    }

    @Test
    fun `'command' is not null`() {
        knot = knot {
            state {
                initial = State()
                reduce { _, state -> effect(state) }
            }
        }
        assertThat(knot.command).isNotNull()
    }

    @Test
    fun `'state' contains initial state`() {
        val state = State()
        knot = knot {
            state {
                initial = state
                reduce { _, state -> effect(state) }
            }
        }
        val observer = knot.state.test()
        observer.assertValue(state)
    }

    @Test
    fun `'currentState' contains initial state`() {
        val state = State()
        knot = knot {
            state {
                initial = state
                reduce { _, state -> effect(state) }
            }
        }
        assertThat(knot.currentState).isEqualTo(state)
    }

    @Test
    fun `'currentState' is updated with the value 'reduce()' returns`() {
        knot = knot {
            state {
                initial = State()
                reduce { _, _ -> effect(State(value = 1)) }
            }
            onCommand { it.map { Change } }
        }
        knot.command.accept(Command)
        assertThat(knot.currentState).isEqualTo(State(1))
    }

    @Test
    fun `'state' is updated with the value 'reduce()' returns`() {
        knot = knot {
            state {
                initial = State()
                reduce { _, _ -> effect(State(value = 1)) }
            }
            onCommand { it.map { Change } }
        }
        val observable = knot.state.test()
        knot.command.accept(Command)
        observable.assertValues(State(0), State(1))
    }

    @Test
    fun `'onEvent' transformer gets invoked on initialization`() {
        val eventTransformer: EventTransformer<Change> = mock {
            on { invoke() }.thenAnswer { Observable.just(Change) }
        }

        knot = knot {
            state {
                initial = State()
                reduce { _, state -> effect(state) }
            }
            onEvent(eventTransformer)
        }
        verify(eventTransformer).invoke()
    }

    @Test
    fun `'onCommand' transformer gets invoked on initialization`() {
        var command: Observable<Command>? = null
        val commandTransformer: CommandTransformer<Command, Change> = mock {
            on { invoke(any()) }.thenAnswer { invocation ->
                invocation.arguments?.let { arguments ->
                    @Suppress("UNCHECKED_CAST")
                    command = arguments[0] as Observable<Command>
                }
                Observable.just(Change)
            }
        }

        knot = knot {
            state {
                initial = State()
                reduce { _, state -> effect(state) }
            }
            onCommand(commandTransformer)
        }

        assertThat(command).isNotNull()
        verify(commandTransformer).invoke(checkNotNull(command))
    }
}
