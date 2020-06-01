package de.halfbit.knot3.sample.books

import androidx.lifecycle.ViewModel
import de.halfbit.knot3.knot
import de.halfbit.knot3.sample.books.model.Action
import de.halfbit.knot3.sample.books.model.Change
import de.halfbit.knot3.sample.books.model.Event
import de.halfbit.knot3.sample.books.model.State
import de.halfbit.knot3.sample.books.model.types.Book
import de.halfbit.knot3.sample.common.actions.DefaultLoadBooksAction
import de.halfbit.knot3.sample.common.actions.LoadBooksAction
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.functions.Consumer

abstract class BooksViewModel : ViewModel() {
    abstract val state: Observable<State>
    abstract val event: Consumer<Event>
}

internal class DefaultBooksViewModel(
    private val loadBooksAction: LoadBooksAction = DefaultLoadBooksAction(),
    private val observeOnScheduler: Scheduler = AndroidSchedulers.mainThread()
) : BooksViewModel() {

    override val state: Observable<State>
        get() = knot.state

    override val event: Consumer<Event> =
        Consumer<Event> { knot.change.accept(it.toChange()) }

    private val knot = knot<State, Change, Action> {
        state {
            initial = State.Empty
            observeOn = observeOnScheduler
        }
        changes {
            reduce { change ->
                when (change) {
                    Change.Load -> when (this) {
                        State.Empty,
                        is State.Content,
                        is State.Error -> State.Loading + Action.Load
                        else -> only
                    }

                    is Change.Load.Success -> when (this) {
                        State.Loading -> State.Content(change.books).only
                        else -> unexpected(change)
                    }

                    is Change.Load.Failure -> when (this) {
                        State.Loading -> State.Error(change.message).only
                        else -> unexpected(change)
                    }

                    Change.Clean -> when (this) {
                        is State.Content -> State.Empty.only
                        is State.Empty -> only
                        else -> unexpected(change)
                    }
                }
            }
        }
        actions {
            perform<Action.Load> {
                switchMapSingle {
                    loadBooksAction.perform()
                        .map { it.toChange() }
                }
            }
        }
    }
}

private fun Event.toChange(): Change = when (this) {
    is Event.Refresh -> Change.Load
    Event.Clear -> Change.Clean
}

private fun LoadBooksAction.Result.toChange() =
    when (this) {
        is LoadBooksAction.Result.Success ->
            Change.Load.Success(books.map { it.toBook() })
        is LoadBooksAction.Result.Failure.Network ->
            Change.Load.Failure("Network error. Check Internet connection and try again.")
        LoadBooksAction.Result.Failure.Generic ->
            Change.Load.Failure("Generic error, please try again.")
    }

private fun LoadBooksAction.Book.toBook() = Book(title, year)
