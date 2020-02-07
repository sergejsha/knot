package de.halfbit.knot.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

class MainViewModel(
    stateObserver: Observable<State>,
    private val changeConsumer: Consumer<Change>,
    private val disposable: Disposable
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
        disposable.dispose()
    }
}

internal val mainViewModelFactory: ViewModelProvider.Factory
    get() = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                createMainKnot().let { knot ->
                    @Suppress("UNCHECKED_CAST")
                    MainViewModel(knot.state, knot.change, knot) as T
                }
            } else error("Unsupported model type: $modelClass")
    }
