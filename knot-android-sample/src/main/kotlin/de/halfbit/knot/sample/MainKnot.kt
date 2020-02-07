package de.halfbit.knot.sample

import de.halfbit.knot.Knot
import de.halfbit.knot.knot
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers

fun createMainKnot(
    stateObserver: Scheduler = AndroidSchedulers.mainThread(),
    loadAction: (Observable<Action.Load>) -> Observable<Change> = createLoadAction()
): Knot<State, Change> {
    return knot<State, Change, Action> {
        state {
            initial = State.Initial

            // observe state on Main thread
            observeOn = stateObserver
        }
        changes {
            reduce { change ->
                // this is the current state
                when (change) {
                    Change.Load -> when (this) {
                        State.Initial -> State.Loading + Action.Load

                        // By returning "only" (the same state we received) from
                        // here we ignore every follow up events coming from the
                        // button.

                        // If we used "unexpected(change)" here, the app would
                        // crash if the user succeeded to press "Load Movies"
                        // button multiple times quick enough.
                        else -> only
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
            perform<Action.Load> { loadAction(this) }
        }
    }
}

sealed class State {
    object Initial : State()
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