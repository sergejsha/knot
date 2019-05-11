package de.halfbit.knot

import org.junit.Test

class CompositeKnotTest {

    private object State
    private object Change
    private object Action

    @Test(expected = IllegalStateException::class)
    fun `DSL builder requires initial state`() {
        compositeKnot<State, Change, Action> {}
    }

    @Test
    fun `DSL builder creates CompositeKnot`() {
        compositeKnot<State, Change, Action> {
            state {
                initial = State
            }
        }
    }

    @Test
    fun `When not composed, knot doesn't dispatch initial state`() {
        val knot = compositeKnot<State, Change, Action> {
            state {
                initial = State
            }
        }
        val observer = knot.state.test()
        observer.assertNoValues()
    }

    @Test(expected = IllegalStateException::class)
    fun `When not composed, knot fails to accept changes`() {
        val knot = compositeKnot<State, Change, Action> {
            state {
                initial = State
            }
        }
        knot.change.accept(Change)
    }

    @Test
    fun `When not composed, knot can be composed`() {
        val knot = compositeKnot<State, Change, Action> {
            state {
                initial = State
            }
        }
        knot.compose(Composition())
    }

    @Test(expected = IllegalStateException::class)
    fun `When composed, knot fails to accept new composition`() {
        val knot = compositeKnot<State, Change, Action> {
            state {
                initial = State
            }
        }
        knot.compose(Composition())
        knot.compose(Composition())
    }

}