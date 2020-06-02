package de.halfbit.knot3.sample.books2.model

sealed class Event {
    object Refresh : Event()
    object Clear : Event()
}
