package de.halfbit.knot

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
}
