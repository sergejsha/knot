package de.halfbit.knot

import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Test

class KnotInterceptActionTest {

    private object State
    private sealed class Change {
        object PerformAction : Change()
        object Done : Change()
    }

    private object Action

    @Test
    fun `actions { intercept } receives Action with performers`() {
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
                perform<Action> { map { Change.Done } }
                perform<Action> { map { Change.Done } }
                intercept { action -> action.doOnNext { interceptor.onNext(it) } }
            }
        }
        knot.change.accept(Change.PerformAction)
        observer.assertValues(Action)
    }

    @Test
    fun `actions { intercept } receives Action without performers`() {
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
                intercept { action -> action.doOnNext { interceptor.onNext(it) } }
            }
        }
        knot.change.accept(Change.PerformAction)
        observer.assertValues(Action)
    }
}