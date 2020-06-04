package de.halfbit.knot3.sample.books2.delegates

import de.halfbit.knot3.CompositeKnot
import de.halfbit.knot3.sample.books2.Delegate
import de.halfbit.knot3.sample.books2.model.Event
import de.halfbit.knot3.sample.books2.model.State

class ClearButtonDelegate : Delegate {
    override fun CompositeKnot<State>.register() {
        registerPrime<Change, Nothing> {
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

    override fun CompositeKnot<State>.onEvent(event: Event) =
        if (event == Event.Clear) {
            change.accept(Change.Clear)
            true
        } else false

    private sealed class Change {
        object Clear : Change()
    }
}