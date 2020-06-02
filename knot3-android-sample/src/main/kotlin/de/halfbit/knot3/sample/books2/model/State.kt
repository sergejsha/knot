package de.halfbit.knot3.sample.books2.model

import de.halfbit.knot3.sample.books2.model.types.Book

sealed class State {
    object Empty : State()
    object Loading : State()
    data class Content(val books: List<Book>) : State()
    data class Error(val message: String) : State()
}
