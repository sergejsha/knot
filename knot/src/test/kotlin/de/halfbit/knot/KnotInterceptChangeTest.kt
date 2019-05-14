package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class KnotInterceptChangeTest {

    private object State

    private sealed class Change {
        object One : Change()
        object Two : Change()
    }

    private object Action

    @Test
    fun `changes { intercept } receives external Change`() {
        val interceptor = PublishSubject.create<Change>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this.only }
                intercept { change -> change.doOnNext { interceptor.onNext(it) } }
            }
        }
        knot.change.accept(Change.One)
        knot.change.accept(Change.Two)
        observer.assertValues(
            Change.One,
            Change.Two
        )
    }

    @Test
    fun `changes { intercept } receives change from Action`() {
        val interceptor = PublishSubject.create<Change>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.One -> this + Action
                        is Change.Two -> this.only
                    }
                }
                intercept { change -> change.doOnNext { interceptor.onNext(it) } }
            }
            actions {
                perform<Action> { it.flatMap { Observable.fromArray(Change.Two, Change.Two) } }
            }
        }
        knot.change.accept(Change.One)
        observer.assertValues(
            Change.One,
            Change.Two,
            Change.Two
        )
    }

    @Test
    fun `changes { intercept } receives change from Event`() {
        val externalSource = PublishSubject.create<Unit>()
        val interceptor = PublishSubject.create<Change>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this.only }
                intercept { change -> change.doOnNext { interceptor.onNext(it) } }
            }
            events {
                transform { externalSource.map { Change.Two } }
            }
        }
        knot.change.accept(Change.One)
        externalSource.onNext(Unit)
        observer.assertValues(
            Change.One,
            Change.Two
        )
    }

    @Test
    fun `intercept { changes } receives external Change`() {
        val interceptor = PublishSubject.create<Change>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this.only }
            }
            intercept {
                changes { change -> change.doOnNext { interceptor.onNext(it) } }
            }
        }
        knot.change.accept(Change.One)
        knot.change.accept(Change.Two)
        observer.assertValues(
            Change.One,
            Change.Two
        )
    }

    @Test
    fun `intercept { changes } receives change from Action`() {
        val interceptor = PublishSubject.create<Change>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { change ->
                    when (change) {
                        is Change.One -> this + Action
                        is Change.Two -> this.only
                    }
                }
            }
            actions {
                perform<Action> { it.flatMap { Observable.fromArray(Change.Two, Change.Two) } }
            }
            intercept {
                changes { change -> change.doOnNext { interceptor.onNext(it) } }
            }
        }
        knot.change.accept(Change.One)
        observer.assertValues(
            Change.One,
            Change.Two,
            Change.Two
        )
    }

    @Test
    fun `intercept { changes } receives change from Event`() {
        val externalSource = PublishSubject.create<Unit>()
        val interceptor = PublishSubject.create<Change>()
        val observer = interceptor.test()
        val knot = knot<State, Change, Action> {
            state {
                initial = State
            }
            changes {
                reduce { this.only }
            }
            events {
                transform { externalSource.map { Change.Two } }
            }
            intercept {
                changes { change -> change.doOnNext { interceptor.onNext(it) } }
            }
        }
        knot.change.accept(Change.One)
        externalSource.onNext(Unit)
        observer.assertValues(
            Change.One,
            Change.Two
        )
    }

}