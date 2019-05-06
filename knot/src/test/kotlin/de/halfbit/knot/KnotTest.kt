package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Observable
import org.junit.Test

class KnotTest {

    private data class State(val value: Int = 0)
    private object Change
    private object Action

    private lateinit var knot: Knot<State, Change, Action>

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
                reduce { _, state -> Effect(state) }
            }
        }
        assertThat(knot).isNotNull()
    }

    @Test
    fun `'state' is not null`() {
        knot = knot {
            state {
                initial = State()
                reduce { _, state -> Effect(state) }
            }
        }
        assertThat(knot.state).isNotNull()
    }

    @Test
    fun `'state' contains initial state`() {
        val state = State()
        knot = knot {
            state {
                initial = state
                reduce { _, state -> Effect(state) }
            }
        }
        val observer = knot.state.test()
        observer.assertValue(state)
    }

    @Test
    fun `'reduce()' updates 'state'`() {
        knot = knot {
            state {
                initial = State()
                reduce { _, _ -> Effect(State(1)) }
            }
        }
        val observable = knot.state.test()
        knot.change.accept(Change)
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
                reduce { _, state -> Effect(state) }
            }
            event {
                transform(eventTransformer)
            }
        }
        verify(eventTransformer).invoke()
    }

}
