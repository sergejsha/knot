package de.halfbit.knot.sample

import androidx.lifecycle.ViewModel
import de.halfbit.knot.Knot
import io.reactivex.Observable

class MainViewModel(
    private val knot: Knot<State, Change> = mainKnotFactory()
) : ViewModel() {

    val showButton: Observable<Boolean> = knot.state
        .map { it == State.Initial }
        .distinctUntilChanged()

    val showLoading: Observable<Boolean> = knot.state
        .map { it == State.Loading }
        .distinctUntilChanged()

    val showMovies: Observable<List<Movie>> = knot.state
        .ofType(State.Ready::class.java)
        .map { it.movies }
        .distinctUntilChanged()

    fun onButtonClick() {
        knot.change.accept(Change.Load)
    }

    override fun onCleared() {
        super.onCleared()
        knot.dispose()
    }
}