package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class PrimeOnEnterStateTest {

    private sealed class State {
        data class Empty(val loading: Boolean) : State()
        object Content : State()
    }

    private sealed class Change {
        object Load : Change() {
            object Done : Change()
        }
    }

    private sealed class Action {
        object Load : Action()
    }

    @Test
    fun `state { onEnter } called for initial state`() {
        val stateObserver = PublishSubject.create<State>()
        val changeOnEnter = { state: Observable<State> ->
            state
                .doOnNext { stateObserver.onNext(it) }
                .map { Change.Load as Change }
        }

        val composition = Composition<State, Change, Action>().apply {
            reducers[Change.Load::class] = { Effect(this) }
            stateTriggers += OnEnterStateTrigger(State.Empty::class.java, changeOnEnter)
        }

        val knot = compositeKnot<State, Change, Action> {
            state { initial = State.Empty(loading = false) }
        }

        val observer = stateObserver.test()
        knot.state.test()
        knot.compose(composition)

        observer.assertValues(
            State.Empty(loading = false)
        )
    }

    @Test
    fun `state { onEnter } not called for same state type`() {
        val stateObserver = PublishSubject.create<State>()
        val changeOnEnter = { state: Observable<State> ->
            state
                .doOnNext { stateObserver.onNext(it) }
                .map { Change.Load as Change }
        }

        val composition = Composition<State, Change, Action>().apply {
            reducers[Change.Load::class] = {
                when (this) {
                    is State.Empty -> Effect(copy(loading = true))
                    else -> error("unexpected $it in $this")
                }
            }
            stateTriggers += OnEnterStateTrigger(State.Empty::class.java, changeOnEnter)
        }

        val knot = compositeKnot<State, Change, Action> {
            state {
                initial = State.Empty(loading = false)
            }
        }

        val enteredStates = stateObserver.test()
        val observedStates = knot.state.test()
        knot.compose(composition)

        observedStates.assertValues(
            State.Empty(loading = false),
            State.Empty(loading = true)
        )

        enteredStates.assertValues(
            State.Empty(loading = false)
        )
    }

    @Test
    fun `state { onEnter } called when state type changes`() {
        val stateObserver = PublishSubject.create<State>()
        val changeOnEnterEmpty = { state: Observable<State> ->
            state
                .doOnNext { stateObserver.onNext(it) }
                .map { Change.Load as Change }
        }

        val composition = Composition<State, Change, Action>().apply {
            actionTransformers += { action: Observable<Action> ->
                action
                    .filter { it is Action.Load }
                    .map { Change.Load.Done }
            }
            reducers[Change.Load::class] = {
                when (this) {
                    is State.Empty -> Effect(copy(loading = true), Action.Load)
                    else -> error("unexpected $it in $this")
                }
            }
            reducers[Change.Load.Done::class] = { Effect(State.Content) }
            stateTriggers += OnEnterStateTrigger(State.Empty::class.java, changeOnEnterEmpty)
        }

        val knot = compositeKnot<State, Change, Action> {
            state {
                initial = State.Empty(loading = false)
            }
        }

        val enteredStates = stateObserver.test()
        val observedStates = knot.state.test()
        knot.compose(composition)

        observedStates.assertValues(
            State.Empty(loading = false),
            State.Empty(loading = true),
            State.Content
        )

        enteredStates.assertValues(
            State.Empty(loading = false)
        )
    }

}