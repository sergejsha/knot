package de.halfbit.knot.sample

import com.google.common.truth.Truth
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class MainViewModelTest {

    private val state = PublishSubject.create<State>()
    private val changeConsumer = ChangeConsumer()
    private val disposable = EmptyDisposable()
    private val model = MainViewModel(state, changeConsumer, disposable)
    private val buttonShowObserver = model.showButton.test()
    private val loadingShowObserver = model.showLoading.test()
    private val moviesShowObserver = model.showMovies.test()

    @Test
    fun `on StateInitial binding observers should emit correct values`() {
        state.onNext(State.Initial)

        buttonShowObserver.assertValue(true)
        loadingShowObserver.assertValue(false)
        moviesShowObserver.assertNoValues()
    }

    @Test
    fun `on StateLoading binding observers should emit correct values`() {
        state.onNext(State.Loading)

        buttonShowObserver.assertValue(false)
        loadingShowObserver.assertValue(true)
        moviesShowObserver.assertNoValues()
    }

    @Test
    fun `on StateError binding observers should emit correct values`() {
        state.onNext(State.Error)

        buttonShowObserver.assertValue(false)
        loadingShowObserver.assertValue(false)
        moviesShowObserver.assertNoValues()
    }

    @Test
    fun `on StateReady binding observers should emit correct values`() {
        val movies = listOf(Movie("Suicide Squad"))
        state.onNext(State.Ready(movies))

        buttonShowObserver.assertValue(false)
        loadingShowObserver.assertValue(false)
        moviesShowObserver.assertValue(movies)
    }

    @Test
    fun `on button click should emit change load`() {
        model.onButtonClick()

        Truth.assertThat(changeConsumer.changes).containsExactly(Change.Load)
    }
}

private class ChangeConsumer : Consumer<Change> {
    val changes: MutableList<Change> = mutableListOf()

    override fun accept(t: Change?) {
        t?.let { changes.add(t) }
    }

}

private class EmptyDisposable : Disposable {
    override fun isDisposed(): Boolean {
        return true
    }

    override fun dispose() {
        //
    }

}