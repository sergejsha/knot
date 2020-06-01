package de.halfbit.knot3.sample.books.model

import de.halfbit.knot3.sample.books.model.types.Book

sealed class Change {
    object Load : Change() {
        data class Success(val books: List<Book>) : Change()
        data class Failure(val message: String) : Change()
    }

    object Clean : Change()
}
