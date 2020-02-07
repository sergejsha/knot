package de.halfbit.knot.sample

import com.google.common.truth.Truth
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test

internal typealias LoadAction = (Observable<Action.Load>) -> Observable<Change>

class MainKnotTest {

    private fun loadAction(observable: Observable<Action.Load>): Observable<Change> =
        observable.map { Change.Load.Failure }

    @Test
    fun `on knot ChangeLoad got accepted we will go into StateLoading`() {
        val knot = createKnot()
        val stateObserver = knot.state.test()

        knot.change.accept(Change.Load)

        Truth.assertThat(stateObserver.values()).contains(State.Loading)
    }

    @Test
    fun `on knot ChangeLoad got accepted the loadMoviesAction got called`() {
        var moviesActionWasCalled = false
        val mockLoadMoviesAction: LoadAction = {
            moviesActionWasCalled = true
            it.map { Change.Load.Failure }
        }
        val knot = createKnot(mockLoadMoviesAction)

        knot.change.accept(Change.Load)

        Truth.assertThat(moviesActionWasCalled).isTrue()
    }

    @Test
    fun `on knot ChangeLoad got accepted we will go into StateError when load movies action fails`() {
        val knot = createKnot()
        val stateObserver = knot.state.test()

        knot.change.accept(Change.Load)

        Truth.assertThat(stateObserver.values().last()).isEqualTo(State.Error)
    }

    @Test
    fun `on knot ChangeLoad got accepted we will go into StateReady when load movies action succeeded`() {
        val loadMoviesAction: LoadAction = {
            it.map { Change.Load.Success(emptyList()) }
        }
        val knot = createKnot(loadMoviesAction)
        val stateObserver = knot.state.test()

        knot.change.accept(Change.Load)

        Truth.assertThat(stateObserver.values().last()).isInstanceOf(State.Ready::class.java)
    }

    private fun createKnot(loadAction: LoadAction = this::loadAction) =
        createMainKnot(Schedulers.trampoline(), loadAction)
}
