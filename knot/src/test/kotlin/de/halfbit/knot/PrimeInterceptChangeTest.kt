package de.halfbit.knot

import io.reactivex.subjects.PublishSubject
import org.junit.Test

class PrimeInterceptChangeTest {

    private data class State(val value: String)
    private data class ChangeA(val value: String)
    private data class ChangeB(val value: String)
    private object Action

    @Test
    fun `Prime (single) changes { intercept } receives Change`() {
        val watcher = PublishSubject.create<ChangeA>()
        val observer = watcher.test()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<ChangeA, Action> {
            changes {
                reduce<ChangeA> { this + Action }
                intercept<ChangeA> { change -> change.doOnNext { watcher.onNext(it) } }
            }
        }
        knot.compose()
        knot.change.accept(ChangeA("changed"))
        observer.assertValues(ChangeA("changed"))
    }

    @Test
    fun `Prime (many) changes { intercept } receives Change`() {
        val watcher = PublishSubject.create<Any>()
        val observer = watcher.test()

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<ChangeA, Action> {
            changes {
                reduce<ChangeA> { only }
                intercept<ChangeA> { change ->
                    change.doOnNext {
                        watcher.onNext(it)
                    }
                }
            }
        }

        knot.registerPrime<ChangeB, Action> {
            changes {
                reduce<ChangeB> { only }
                intercept<ChangeB> { change ->
                    change.doOnNext {
                        watcher.onNext(it)
                    }
                }
            }
        }

        knot.compose()
        knot.change.accept(ChangeA("changed"))
        knot.change.accept(ChangeB("changed"))
        observer.assertValues(
            ChangeA("changed"),
            ChangeB("changed")
        )
    }
}