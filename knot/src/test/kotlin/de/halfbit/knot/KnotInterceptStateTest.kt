package de.halfbit.knot

import io.reactivex.subjects.PublishSubject
import org.junit.Test

class KnotInterceptStateTest {

    private sealed class State {
        object Zero : State()
        object One : State()
    }

    private sealed class Change {
        object One : Change()
    }

    private object Action

    @Test
    fun `state { intercept } receives initial State`() {
        val interceptor = PublishSubject.create<State>()
        val observer = interceptor.test()
        knot<State, Change, Action> {
            state {
                initial = State.Zero
                intercept { state -> state.doOnNext { interceptor.onNext(it) } }
            }
            changes {
                reduce { this.only }
            }
        }
        observer.assertValues(State.Zero)
    }

    @Test
    fun `state { intercept } receives State mutations`() {
        val interceptor = PublishSubject.create<State>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State.Zero
                intercept { state -> state.doOnNext { interceptor.onNext(it) } }
            }
            changes {
                reduce { change ->
                    when (change) {
                        Change.One -> State.One.only
                    }
                }
            }
        }
        knot.change.accept(Change.One)
        observer.assertValues(
            State.Zero,
            State.One
        )
    }

    @Test
    fun `state { intercept } receives State mutations in multiple statements`() {
        val interceptor = PublishSubject.create<State>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State.Zero
                intercept { state -> state.doOnNext { interceptor.onNext(it) } }
                intercept { state -> state.doOnNext { interceptor.onNext(it) } }
            }
            changes {
                reduce { change ->
                    when (change) {
                        Change.One -> State.One.only
                    }
                }
            }
        }
        knot.change.accept(Change.One)
        observer.assertValues(
            State.Zero,
            State.Zero,
            State.One,
            State.One
        )
    }
}