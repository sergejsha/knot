package de.halfbit.knot

import de.halfbit.knot.dsl.Reducer
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConcurrentStateUpdate {

    object CountUpCommand
    data class State(val counter: Int = 0) {
        override fun toString(): String = "State: $counter"
    }

    private lateinit var knot: Knot<State, CountUpCommand>

    @Test
    fun `Concurrent state updates get serialized`() {

        val latch = CountDownLatch(2 * COUNT)
        val countUpEmitter1 = Observable
            .create<Unit> { emitter ->
                for (i in 1..COUNT) {
                    Thread.sleep(10)
                    emitter.onNext(Unit)
                    latch.countDown()
                }
            }
            .subscribeOn(Schedulers.newThread())

        val countUpEmitter2 = Observable
            .create<Unit> { emitter ->
                for (i in 1..COUNT) {
                    Thread.sleep(10)
                    emitter.onNext(Unit)
                    latch.countDown()
                }
            }
            .subscribeOn(Schedulers.newThread())

        knot = tieKnot {
            state { initial = State() }

            on(countUpEmitter1) {
                updateState {
                    it.map<Reducer<State>> {
                        reduceState { state.copy(counter = state.counter + 1) }
                    }
                }
            }

            on(countUpEmitter2) {
                updateState {
                    it.map<Reducer<State>> {
                        reduceState { state.copy(counter = state.counter + 100) }
                    }
                }
            }
        }

        latch.await(3, TimeUnit.SECONDS)

        val observer = knot.state.test()
        observer.assertValues(
            State(COUNT * 100 + COUNT)
        )
    }

}

private const val COUNT = 25