package de.halfbit.knot.sample

import com.google.common.truth.Truth
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.Test

class MainKnotTest {

    private val testScheduler = TestScheduler()

    private val loadAction: (Observable<Action.Load>) -> Observable<Change> = {
        it.map { Change.Load.Fail }
    }

    @Test
    fun `on knot ChangeLoad got accepted we will go into StateLoading`() {
        val knot = createKnot()
        val stateObserver = knot.state.test()

        knot.change.accept(Change.Load)

        testScheduler.triggerActions()
        Truth.assertThat(stateObserver.values()).contains(State.Loading)
    }

    @Test
    fun `on knot ChangeLoad got accepted the loadMoviesAction got called`() {
        var moviesActionWasCalled = false
        val mockLoadMoviesAction: (Observable<Action.Load>) -> Observable<Change> = {
            moviesActionWasCalled = true
            it.map { Change.Load.Fail }
        }
        val knot = createKnot(mockLoadMoviesAction)

        knot.change.accept(Change.Load)

        testScheduler.triggerActions()
        Truth.assertThat(moviesActionWasCalled).isTrue()
    }

    @Test
    fun `on knot ChangeLoad got accepted we will go into StateError when load movies action fails`() {
        val knot = createKnot()
        val stateObserver = knot.state.test()

        knot.change.accept(Change.Load)

        testScheduler.triggerActions()
        Truth.assertThat(stateObserver.values().last()).isEqualTo(State.Error)
    }

    @Test
    fun `on knot ChangeLoad got accepted we will go into StateReady when load movies action succeeded`() {
        val loadMoviesAction: (Observable<Action.Load>) -> Observable<Change> = {
            it.map { Change.Load.Success(emptyList()) }
        }
        val knot = createKnot(loadMoviesAction)
        val stateObserver = knot.state.test()

        knot.change.accept(Change.Load)

        testScheduler.triggerActions()
        Truth.assertThat(stateObserver.values().last()).isInstanceOf(State.Ready::class.java)
    }

    private fun createKnot(loadAction: (Observable<Action.Load>) -> Observable<Change> = this.loadAction) =
        mainKnotFactory(testScheduler, loadAction)
}
