package de.halfbit.knot.sample

import androidx.lifecycle.ViewModel
import de.halfbit.knot.knot
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {

    private val knot = knot<State, Change, Action> {
        state { initial = State.Init }
        changes {
            reduce { change ->
                // this is the current state
                when (change) {
                    Change.Load -> when (this) {
                        State.Init -> State.Loading + Action.Load
                        else -> unexpected(change)
                    }
                    is Change.Load.Success -> when (this) {
                        State.Loading -> State.Ready(change.movies).only
                        else -> unexpected(change)
                    }
                    Change.Load.Fail -> when (this) {
                        State.Loading -> State.Error.only
                        else -> unexpected(change)
                    }
                }
            }
        }
        actions {
            perform<Action.Load> {
                this
                    .delay(
                        3,
                        TimeUnit.SECONDS,
                        AndroidSchedulers.mainThread()
                    ) // To fake the loading
                    .map {
                        // Do a operation to load the movies
                        listOf(Movie("The day after tomorrow"), Movie("Joker"))
                    }
                    .map { movies -> Change.Load.Success(movies) as Change }
                    .onErrorReturn { Change.Load.Fail }
            }
        }
    }

    val showButton: Observable<Boolean> = knot.state
        .map { it == State.Init }
        .distinctUntilChanged()

    val showLoading: Observable<Boolean> = knot.state
        .map { it == State.Loading }
        .distinctUntilChanged()

    val showMovies: Observable<List<Movie>> = knot.state
        .ofType(State.Ready::class.java)
        .map { it.movies }
        .distinctUntilChanged()

    val onButtonClick: () -> Unit = { knot.change.accept(Change.Load) }

    override fun onCleared() {
        super.onCleared()
        knot.dispose()
    }
}

sealed class State {
    object Init : State()
    object Loading : State()
    data class Ready(val movies: List<Movie>) : State()
    object Error : State()
}

sealed class Change {
    object Load : Change() {
        data class Success(val movies: List<Movie>) : Change()
        object Fail : Change()
    }
}

sealed class Action {
    object Load : Action()
}

data class Movie(val title: String)