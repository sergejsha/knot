package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import io.reactivex.schedulers.Schedulers
import org.junit.Test

class CompositeKnotTest {

    private object State

    @Test(expected = IllegalStateException::class)
    fun `DSL builder requires initial state`() {
        compositeKnot<State> {}
    }

    @Test
    fun `DSL builder creates CompositeKnot`() {
        compositeKnot<State> {
            state {
                initial = State
            }
        }
    }

    @Test
    fun `When not composed, knot doesn't dispatch initial state`() {
        val knot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        val observer = knot.state.test()
        observer.assertNoValues()
    }

    @Test
    fun `When not composed, knot can be composed`() {
        val knot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        knot.compose()
    }

    @Test(expected = IllegalStateException::class)
    fun `When composed, knot fails to accept new composition`() {
        val knot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        knot.compose()
        knot.compose()
    }

    @Test
    fun `state { observeOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from { visited = true; it.run() }
        val knot = compositeKnot<State> {
            state {
                initial = State
                observeOn = scheduler
            }
        }
        knot.state.test()
        knot.compose()
        assertThat(visited).isTrue()
    }

    @Test
    fun `state { watchOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from { visited = true; it.run() }
        val knot = compositeKnot<State> {
            state {
                initial = State
                watchOn = scheduler
                watchAll { }
            }
        }
        knot.state.test()
        knot.compose()
        assertThat(visited).isTrue()
    }

    @Test
    fun `state { watchOn } is null by default`() {
        val knot = compositeKnot<State> {
            state {
                initial = State
                assertThat(watchOn).isNull()
            }
        }
        knot.compose()
    }

    @Test(expected = IllegalStateException::class)
    fun `state { watchOn } fails if declared after a watcher`() {
        compositeKnot<State> {
            state {
                initial = State
                watchAll { }
                watchOn = Schedulers.from { }
            }
        }.compose()
    }

    @Test(expected = IllegalStateException::class)
    fun `TestCompositeKnot fails emitting changes when knot is not composed`() {
        val knot = compositeKnot<State> {
            state { initial = State }
        }
        knot.change.accept(Unit)
    }
}
