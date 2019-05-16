package de.halfbit.knot

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Observable
import org.junit.Test

class KnotTest {

    private data class State(val value: Int = 0)
    private object Change
    private object Action

    private lateinit var knot: Knot<State, Change>

    @Test(expected = IllegalStateException::class)
    fun `DSL builder requires initial state`() {
        knot = knot<State, Change, Action> { }
    }

    @Test(expected = IllegalStateException::class)
    fun `DSL builder requires reducer`() {
        knot = knot<State, Change, Action> {
            state {
                initial = State()
            }
        }
    }

    @Test
    fun `Initial state gets dispatched`() {
        val state = State()
        knot = knot<State, Change, Action> {
            state {
                initial = state
            }
            changes {
                reduce { this.only }
            }
        }
        val observer = knot.state.test()
        observer.assertValue(state)
    }

    @Test
    fun `Reduces updates state`() {
        knot = knot<State, Change, Action> {
            state {
                initial = State()
            }
            changes {
                reduce { State(1).only }
            }
        }
        val observable = knot.state.test()
        knot.change.accept(Change)
        observable.assertValues(State(0), State(1))
    }

    @Test
    fun `Event transformer gets invoked on initialization`() {
        val eventSource: EventSource<Change> = mock {
            on { invoke() }.thenAnswer { Observable.just(Change) }
        }

        knot = knot<State, Change, Action> {
            state {
                initial = State()
            }
            changes {
                reduce { this.only }
            }
            events {
                source(eventSource)
            }
        }
        verify(eventSource).invoke()
    }

    @Test
    fun `Action transformer gets invoked on initialization`() {
        val actionTransformer: ActionTransformer<Action, Change> = mock {
            on { invoke(any()) }.thenAnswer { Observable.just(Change) }
        }

        knot = knot<State, Change, Action> {
            state {
                initial = State()
            }
            changes {
                reduce { this.only }
            }
            actions {
                perform(actionTransformer)
            }
        }
        verify(actionTransformer).invoke(any())
    }

}
