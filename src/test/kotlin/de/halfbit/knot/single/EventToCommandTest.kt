package de.halfbit.knot.single

import de.halfbit.knot.Knot
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class EventToCommandTest {

    private val eventSource = EventSource()
    private lateinit var knot: Knot<State, Command>

    @Test
    fun `Event can be transformed into command`() {

        knot = singleKnot {
            state { initial = State.Unknown }
            onEvent<Event>(eventSource.event) {
                issueCommand { event ->
                    event.map { Command.Load }
                }
            }
            on<Command.Load> {
                reduceState { command ->
                    command.map { State.Loaded }
                }
            }
        }

        val observer = knot.state.test()
        eventSource.event.onNext(Event)

        observer.assertValues(
            State.Unknown,
            State.Loaded
        )
    }

    private sealed class Command {
        object Load : Command()
    }

    private sealed class State {
        object Unknown : State()
        object Loaded : State()
    }

    private object Event
    private class EventSource {
        val event: PublishSubject<Event> = PublishSubject.create()
    }

}