package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test

class KnotTest {

    private data class State(val value: Int = 0)
    private object Change
    private object Action

    @Test(expected = IllegalStateException::class)
    fun `DSL builder requires initial state`() {
        knot<State, Change, Action> { }
    }

    @Test(expected = IllegalStateException::class)
    fun `DSL builder requires reducer`() {
        knot<State, Change, Action> {
            state {
                initial = State()
            }
        }
    }

    @Test
    fun `Initial state gets dispatched`() {
        val state = State()
        val knot = knot<State, Change, Action> {
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
        val knot = knot<State, Change, Action> {
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

        knot<State, Change, Action> {
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

        knot<State, Change, Action> {
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

    @Test
    fun `state { observeOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from {
            visited = true
            it.run()
        }
        val knot = knot<State, Change, Action> {
            state {
                initial = State()
                observeOn = scheduler
            }
            changes {
                reduce { this.only }
            }
        }

        knot.state.test()
        assertThat(visited).isTrue()
    }

    @Test
    fun `changes { reduceOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from {
            visited = true
            it.run()
        }
        val knot = knot<State, Change, Action> {
            state {
                initial = State()
            }
            changes {
                reduce { this.only }
                reduceOn = scheduler
            }
        }

        knot.state.test()
        knot.change.accept(Change)
        assertThat(visited).isTrue()
    }
}
