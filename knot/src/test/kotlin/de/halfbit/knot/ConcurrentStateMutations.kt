package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConcurrentStateMutations {

    data class CountUpChange(val value: Int)

    data class State(val counter: Int = 0) {
        override fun toString(): String = "State: $counter"
    }

    private lateinit var knot: Knot<State, CountUpChange>

    @Test
    fun `Concurrent state updates are serialized`() {

        val latch = CountDownLatch(2 * COUNT)

        val countUpEmitter = Observable
            .create<Unit> { emitter ->
                for (i in 1..COUNT) {
                    Thread.sleep(DELAY_EMITTER1)
                    emitter.onNext(Unit)
                    latch.countDown()
                }
            }
            .subscribeOn(Schedulers.newThread())

        knot = knot<State, CountUpChange, Unit> {
            state {
                initial = State()
            }
            changes {
                reduce { copy(counter = counter + it.value).only }
            }
            events {
                transform { countUpEmitter.map { CountUpChange(100) } }
            }
        }

        Observable
            .create<CountUpChange> { emitter ->
                for (i in 1..COUNT) {
                    Thread.sleep(DELAY_EMITTER2)
                    emitter.onNext(CountUpChange(1))
                    latch.countDown()
                }
            }
            .subscribeOn(Schedulers.newThread())
            .subscribe(knot.change)

        latch.await(3, TimeUnit.SECONDS)

        val observer = knot.state.test()
        observer.assertValues(
            State(COUNT * 100 + COUNT)
        )
    }

}

private const val COUNT = 40
private const val DELAY_EMITTER1 = 11L
private const val DELAY_EMITTER2 = 10L