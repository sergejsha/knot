package de.halfbit.knot

import io.reactivex.subjects.PublishSubject
import org.junit.Test

class KnotInterceptActionTest {

    private object State
    private sealed class Change {
        object PerformAction : Change()
        object Done : Change()
    }

    private object Action

    @Test
    fun `actions { intercept } receives Action`() {
        val interceptor = PublishSubject.create<Action>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { change ->
                    when (change) {
                        Change.PerformAction -> this + Action
                        Change.Done -> this.only
                    }
                }
            }
            actions {
                perform<Action> { action -> action.map { Change.Done } }
                intercept { action -> action.doOnNext { interceptor.onNext(it) } }
            }
        }
        knot.change.accept(Change.PerformAction)
        observer.assertValues(Action)
    }

    @Test
    fun `intercept { action } receives Action`() {
        val interceptor = PublishSubject.create<Action>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { change ->
                    when (change) {
                        Change.PerformAction -> this + Action
                        Change.Done -> this.only
                    }
                }
            }
            actions {
                perform<Action> { action -> action.map { Change.Done } }
            }
            intercept {
                action { action -> action.doOnNext { interceptor.onNext(it) } }
            }
        }
        knot.change.accept(Change.PerformAction)
        observer.assertValues(Action)
    }

}