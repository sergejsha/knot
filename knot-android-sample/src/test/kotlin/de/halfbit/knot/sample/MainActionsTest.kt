package de.halfbit.knot.sample

import com.google.common.truth.Truth
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.junit.Test
import java.util.concurrent.TimeUnit

class MainActionsTest {

    private val testScheduler = TestScheduler()

    @Test
    fun `when action get called successfully it will be emit ChangeLoadSuccess`() {
        val action = createLoadAction(testScheduler)

        val testObserver = action(Observable.just(Action.Load)).test()

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        val change = testObserver.values().first()
        Truth.assertThat(change).isInstanceOf(Change.Load.Success::class.java)
        val successChange = change as Change.Load.Success
        Truth.assertThat(successChange.movies).isNotEmpty()
    }

    @Test
    fun `action action get called with an error it will emit ChangeLoadFail`() {
        val action = createLoadAction(testScheduler)

        val testObserver = action(Observable.error(Throwable())).test()

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        val change = testObserver.values().first()
        Truth.assertThat(change).isEqualTo(Change.Load.Failure)
    }
}