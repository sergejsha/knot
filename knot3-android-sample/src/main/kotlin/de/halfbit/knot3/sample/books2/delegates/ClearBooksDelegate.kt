package de.halfbit.knot3.sample.books2.delegates

import de.halfbit.knot3.CompositeKnot
import de.halfbit.knot3.sample.books2.Delegate
import de.halfbit.knot3.sample.books2.model.Event
import de.halfbit.knot3.sample.books2.model.State

class ClearBooksDelegate : Delegate {
    override fun register(knot: CompositeKnot<State>) {
        knot.registerPrime<Change, Nothing> {
            changes {
                reduce<Change.Clear> { change ->
                    when (this) {
                        is State.Content -> State.Empty.only
                        is State.Empty -> only
                        else -> unexpected(change)
                    }
                }
            }
        }
    }

    override fun CompositeKnot<State>.onEvent(event: Event) {
        if (event is Event.Clear) {
            change.accept(Change.Clear)
        }
    }

    // Sealed class is used for adding some structure the changes.
    // It can also be omitted.

    private sealed class Change {
        object Clear : Change()
    }
}