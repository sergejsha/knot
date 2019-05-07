package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class PrimeTest {

    private object State

    private sealed class Change {
        object One : Change()
        object Two : Change()
    }

    private sealed class Action {
        object One : Action()
        object Two : Action()
    }

    @Test
    fun `Prime adds reducers to composition`() {
        val reducerOne = { state: State, _: Change -> Effect<State, Action>(state) }
        val reducerTwo = { state: State, _: Change -> Effect<State, Action>(state) }

        val prime = prime<State, Change, Action> {
            state {
                reduce<Change.One>(reducerOne)
                reduce<Change.Two>(reducerTwo)
            }
        }

        val composition = Composition<State, Change, Action>()
        prime.addTo(composition)

        assertThat(composition.reducers).containsExactly(
            Change.One::class, reducerOne,
            Change.Two::class, reducerTwo
        )
    }

    @Test
    fun `Prime adds action transformers to composition`() {
        val prime = prime<State, Change, Action> {
            action {
                perform { action: Observable<Action.One> -> action.map<Change> { Change.One } }
                perform { action: Observable<Action.Two> -> action.map<Change> { Change.Two } }
            }
        }

        val composition = Composition<State, Change, Action>()
        prime.addTo(composition)

        assertThat(composition.actionTransformers).hasSize(2)

        val subject = PublishSubject.create<Action>()
        val observer1 = composition.actionTransformers[0].invoke(subject).test()
        val observer2 = composition.actionTransformers[1].invoke(subject).test()

        subject.onNext(Action.One)
        observer1.assertValues(Change.One)
        observer2.assertNoValues()

        subject.onNext(Action.Two)
        observer1.assertValues(Change.One)
        observer2.assertValues(Change.Two)
    }

    @Test
    fun `Prime adds event transformers to composition`() {
        val sourceOne = PublishSubject.create<Unit>()
        val sourceTwo = PublishSubject.create<Unit>()
        val prime = prime<State, Change, Action> {
            event {
                transform { sourceOne.map { Change.One } }
                transform { sourceTwo.map { Change.Two } }
            }
        }

        val composition = Composition<State, Change, Action>()
        prime.addTo(composition)

        assertThat(composition.eventTransformers).hasSize(2)

        val observer1 = composition.eventTransformers[0].invoke().test()
        val observer2 = composition.eventTransformers[1].invoke().test()

        sourceOne.onNext(Unit)
        observer1.assertValues(Change.One)
        observer2.assertNoValues()

        sourceTwo.onNext(Unit)
        observer1.assertValues(Change.One)
        observer2.assertValues(Change.Two)
    }

}