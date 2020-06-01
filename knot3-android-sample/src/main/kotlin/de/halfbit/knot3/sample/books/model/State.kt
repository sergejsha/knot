package de.halfbit.knot3.sample.books.model

import de.halfbit.knot3.sample.books.model.types.Book

sealed class State {
    object Initial : State()
    object Loading : State()
    data class Content(val books: List<Book>) : State()
    data class Error(val message: String) : State()
}
