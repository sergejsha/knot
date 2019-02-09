package de.halfbit.knot

import io.reactivex.subjects.PublishSubject
import org.junit.Test

class EventToCommandTest {

    private val eventSource = EventSource()
    private lateinit var knot: Knot<State, Command>

    @Test
    fun `Event can be transformed into command`() {

        knot = knot {
            state { initial = State.Unknown }
            on(eventSource.event) {
                toCommand {
                    it.map { Command.Load }
                }
            }
            on<Command.Load> {
                reduceState {
                    it.map { State.Loaded }
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