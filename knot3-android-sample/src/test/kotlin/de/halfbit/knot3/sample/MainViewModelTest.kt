package de.halfbit.knot3.sample

import com.google.common.truth.Truth
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Test

class MainViewModelTest {

    private val state = PublishSubject.create<State>()
    private val changeConsumer = ChangeConsumer()
    private val model = MainViewModel(state, changeConsumer, NopDisposable())
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

    override fun accept(t: Change) {
        changes.add(t)
    }
}

private class NopDisposable : Disposable {
    override fun isDisposed(): Boolean = false
    override fun dispose() {
        // nop
    }
}