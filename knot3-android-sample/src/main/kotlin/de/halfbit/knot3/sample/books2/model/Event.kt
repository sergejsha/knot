package de.halfbit.knot3.sample.books2.model

sealed class Event {
    object Load : Event()
    object Clear : Event()
}
