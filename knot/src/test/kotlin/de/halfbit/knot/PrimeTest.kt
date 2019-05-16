package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
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
        val reducerOne = { state: State, _: Change ->
            Effect<State, Action>(
                state
            )
        }
        val reducerTwo = { state: State, _: Change ->
            Effect<State, Action>(
                state
            )
        }

        val prime = prime<State, Change, Action> {
            changes {
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
            actions {
                perform<Action.One> { map<Change> { Change.One } }
                perform<Action.Two> { map<Change> { Change.Two } }
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
            events {
                source { sourceOne.map { Change.One } }
                source { sourceTwo.map { Change.Two } }
            }
        }

        val composition = Composition<State, Change, Action>()
        prime.addTo(composition)

        assertThat(composition.eventSources).hasSize(2)

        val observer1 = composition.eventSources[0].invoke().test()
        val observer2 = composition.eventSources[1].invoke().test()

        sourceOne.onNext(Unit)
        observer1.assertValues(Change.One)
        observer2.assertNoValues()

        sourceTwo.onNext(Unit)
        observer1.assertValues(Change.One)
        observer2.assertValues(Change.Two)
    }

    @Test
    fun `Prime adds state triggers to composition`() {
        val prime = prime<State, Change, Action> {
            state {
                onEnter<State> { state -> state.map { Change.One } }
                onEnter<State> { state -> state.map { Change.Two } }
            }
        }

        val composition = Composition<State, Change, Action>()
        prime.addTo(composition)

        assertThat(composition.stateTriggers).hasSize(2)

        val stateSubject = PublishSubject.create<State>()
        val observer1 = composition.stateTriggers[0].invoke(stateSubject).test()
        val observer2 = composition.stateTriggers[1].invoke(stateSubject).test()

        stateSubject.onNext(State)
        observer1.assertValues(Change.One)
        observer2.assertValues(Change.Two)
    }

}