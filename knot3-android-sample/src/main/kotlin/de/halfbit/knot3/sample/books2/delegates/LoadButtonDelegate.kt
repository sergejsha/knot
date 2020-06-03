package de.halfbit.knot3.sample.books2.delegates

import de.halfbit.knot3.CompositeKnot
import de.halfbit.knot3.sample.books2.Delegate
import de.halfbit.knot3.sample.books2.actions.DefaultLoadBooksAction
import de.halfbit.knot3.sample.books2.actions.LoadBooksAction
import de.halfbit.knot3.sample.books2.model.Action
import de.halfbit.knot3.sample.books2.model.Event
import de.halfbit.knot3.sample.books2.model.State
import de.halfbit.knot3.sample.books2.model.types.Book

class LoadButtonDelegate(
    private val loadBooksAction: LoadBooksAction = DefaultLoadBooksAction()
) : Delegate {

    override fun register(knot: CompositeKnot<State>) {
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.Load> {
                    when (this) {
                        State.Empty,
                        is State.Content,
                        is State.Error -> State.Loading + Load
                        else -> only
                    }
                }
                reduce<Change.Load.Success> { change ->
                    when (this) {
                        State.Loading -> State.Content(change.books).only
                        else -> unexpected(change)
                    }
                }
                reduce<Change.Load.Failure> { change ->
                    when (this) {
                        State.Loading -> State.Error(change.message).only
                        else -> unexpected(change)
                    }
                }
            }
            actions {
                perform<Load> {
                    switchMapSingle {
                        loadBooksAction.perform()
                            .map { it.toChange() }
                    }
                }
            }
        }
    }

    override fun CompositeKnot<State>.onEvent(event: Event) =
        if (event == Event.Load) {
            change.accept(Change.Load)
            true
        } else false

    private sealed class Change {
        object Load : Change() {
            data class Success(val books: List<Book>) : Change()
            data class Failure(val message: String) : Change()
        }
    }

    private object Load : Action

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
}
