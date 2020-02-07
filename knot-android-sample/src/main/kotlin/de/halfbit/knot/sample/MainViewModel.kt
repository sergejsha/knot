package de.halfbit.knot.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

val mainViewModelFactory: ViewModelProvider.Factory
    get() {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                val knot = createMainKnot()
                return modelClass
                    .getConstructor(Observable::class.java, Consumer::class.java, Disposable::class.java)
                    .newInstance(knot.state, knot.change, knot)
            }

        }
    }

class MainViewModel(
    stateObserver: Observable<State>,
    private val changeConsumer: Consumer<Change>,
    private val knotDisposable: Disposable
) : ViewModel() {

    val showButton: Observable<Boolean> = stateObserver
        .map { it == State.Initial }
        .distinctUntilChanged()

    val showLoading: Observable<Boolean> = stateObserver
        .map { it == State.Loading }
        .distinctUntilChanged()

    val showMovies: Observable<List<Movie>> = stateObserver
        .ofType(State.Ready::class.java)
        .map { it.movies }
        .distinctUntilChanged()

    fun onButtonClick() {
        changeConsumer.accept(Change.Load)
    }

    override fun onCleared() {
        super.onCleared()
        knotDisposable.dispose()
    }
}