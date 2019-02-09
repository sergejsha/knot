package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class EventReduceStateTest {

    private val eventSource = EventSource()
    private lateinit var knot: Knot<State, Any>

    @Test
    fun `Event reduces state`() {

        knot = tieKnot {
            state { initial = State.Unknown }
            on(eventSource.event) {
                updateState { event ->
                    event.flatMap<State> {
                        Observable.just(it)
                            .map<State> { ev -> State.Loaded(ev) }
                            .startWith(State.Loading)
                    }
                }
            }
        }

        val observer = knot.state.test()
        eventSource.event.onNext(Event)

        observer.assertValues(
            State.Unknown,
            State.Loading,
            State.Loaded(Event)
        )
    }

    @Test
    fun `Event provides initial state`() {

        knot = tieKnot {
            state { initial = State.Loading }
            on(eventSource.event) {
                updateState { event ->
                    event
                        .filter { state == State.Loading }
                        .map { State.Loaded(it) }
                }
            }
        }

        val observer = knot.state.test()
        eventSource.event.onNext(Event)

        observer.assertValues(
            State.Loading,
            State.Loaded(Event)
        )
    }

    @Test
    fun `Event provides updated state`() {

        knot = tieKnot {
            state { initial = State.Unknown }
            on(eventSource.event) {
                updateState { event ->
                    event.flatMap<State> {
                        Observable.just(it)
                            .map<State> {
                                if (state == State.Loading) State.Loaded(it)
                                else State.Unknown
                            }
                            .startWith(State.Loading)
                    }
                }
            }
        }

        val observer = knot.state.test()
        eventSource.event.onNext(Event)

        observer.assertValues(
            State.Unknown,
            State.Loading,
            State.Loaded(Event)
        )
    }


    private sealed class State {
        object Unknown : State()
        object Loading : State()
        data class Loaded(val event: Event) : State()
    }

    private object Event
    private class EventSource {
        val event: PublishSubject<Event> = PublishSubject.create()
    }

}