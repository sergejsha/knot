package de.halfbit.knot3.sample.books.model

sealed class Event {
    object Load : Event()
    object Clear : Event()
}
