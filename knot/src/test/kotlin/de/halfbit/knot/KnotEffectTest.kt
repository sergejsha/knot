package de.halfbit.knot

import io.reactivex.subjects.PublishSubject
import org.junit.Test

class KnotEffectTest {

    private object State
    private object Change
    private sealed class Action {
        object One : Action()
        object Two : Action()
        object Three : Action()
    }

    @Test
    fun `Knot Effect with null action`() {
        val actions = PublishSubject.create<Action>()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { only }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        val observer = actions.test()
        knot.change.accept(Change)
        observer.assertEmpty()
    }

    @Test
    fun `Knot Effect + null action`() {
        val actions = PublishSubject.create<Action>()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { only + null  }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        val observer = actions.test()
        knot.change.accept(Change)
        observer.assertEmpty()
    }

    @Test
    fun `Knot Effect this + null action`() {
        val actions = PublishSubject.create<Action>()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this + null }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        val observer = actions.test()
        knot.change.accept(Change)
        observer.assertEmpty()
    }

    @Test
    fun `Knot Effect this + one action`() {
        val actions = PublishSubject.create<Action>()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this + Action.One }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        val observer = actions.test()
        knot.change.accept(Change)
        observer.assertValue(Action.One)
    }

    @Test
    fun `Knot Effect this + one action + null`() {
        val actions = PublishSubject.create<Action>()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this + Action.One + null }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        val observer = actions.test()
        knot.change.accept(Change)
        observer.assertValues(Action.One)
    }

    @Test
    fun `Knot Effect only + one action`() {
        val actions = PublishSubject.create<Action>()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { only + Action.One }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        val observer = actions.test()
        knot.change.accept(Change)
        observer.assertValue(Action.One)
    }

    @Test
    fun `Knot Effect this + two actions`() {
        val actions = PublishSubject.create<Action>()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this + Action.One + Action.Two }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        val observer = actions.test()
        knot.change.accept(Change)
        observer.assertValues(
            Action.One, Action.Two
        )
    }

    @Test
    fun `Knot Effect this + two actions + null`() {
        val actions = PublishSubject.create<Action>()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this + Action.One + Action.Two + null }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        val observer = actions.test()
        knot.change.accept(Change)
        observer.assertValues(
            Action.One, Action.Two
        )
    }

    @Test
    fun `Knot Effect this + three actions`() {
        val actions = PublishSubject.create<Action>()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this + Action.One + Action.Two + Action.Three }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        val observer = actions.test()
        knot.change.accept(Change)
        observer.assertValues(
            Action.One, Action.Two, Action.Three
        )
    }

    @Test
    fun `CompositeKnot Effect with null action`() {
        val actions = PublishSubject.create<Action>()
        val compositeKnot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        compositeKnot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { only }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        compositeKnot.compose()
        val observer = actions.test()
        compositeKnot.change.accept(Change)
        observer.assertEmpty()
    }

    @Test
    fun `CompositeKnot Effect + null action`() {
        val actions = PublishSubject.create<Action>()
        val compositeKnot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        compositeKnot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { only + null }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        compositeKnot.compose()
        val observer = actions.test()
        compositeKnot.change.accept(Change)
        observer.assertEmpty()
    }

    @Test
    fun `CompositeKnot Effect this + null action`() {
        val actions = PublishSubject.create<Action>()
        val compositeKnot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        compositeKnot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { this + null }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        compositeKnot.compose()
        val observer = actions.test()
        compositeKnot.change.accept(Change)
        observer.assertEmpty()
    }

    @Test
    fun `CompositeKnot Effect this + one action`() {
        val actions = PublishSubject.create<Action>()
        val compositeKnot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        compositeKnot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { this + Action.One }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        compositeKnot.compose()
        val observer = actions.test()
        compositeKnot.change.accept(Change)
        observer.assertValues(
            Action.One
        )
    }

    @Test
    fun `CompositeKnot Effect only + one action`() {
        val actions = PublishSubject.create<Action>()
        val compositeKnot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        compositeKnot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { only + Action.One }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        compositeKnot.compose()
        val observer = actions.test()
        compositeKnot.change.accept(Change)
        observer.assertValues(
            Action.One
        )
    }

    @Test
    fun `CompositeKnot Effect only + two actions`() {
        val actions = PublishSubject.create<Action>()
        val compositeKnot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        compositeKnot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { this + Action.One + Action.Two }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        compositeKnot.compose()
        val observer = actions.test()
        compositeKnot.change.accept(Change)
        observer.assertValues(
            Action.One, Action.Two
        )
    }

    @Test
    fun `CompositeKnot Effect only + three actions`() {
        val actions = PublishSubject.create<Action>()
        val compositeKnot = compositeKnot<State> {
            state {
                initial = State
            }
        }
        compositeKnot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { this + Action.One + Action.Two + Action.Three }
            }
            actions {
                watchAll { actions.onNext(it) }
            }
        }
        compositeKnot.compose()
        val observer = actions.test()
        compositeKnot.change.accept(Change)
        observer.assertValues(
            Action.One, Action.Two, Action.Three
        )
    }
}
