package de.halfbit.knot

import io.reactivex.Observable
import org.junit.Test

class CommandReduceStateTest {

    private lateinit var knot: Knot<State, Command>

    @Test
    fun `Command reduces state`() {

        knot = knot {
            state { initial = State.Unknown }
            on<Command.Load> {
                reduceState { command ->
                    command.flatMap<State> {
                        Observable.just(it)
                            .map<State> { State.Loaded }
                            .startWith(State.Loading)
                    }
                }
            }
        }

        val observer = knot.state.test()
        knot.command.accept(Command.Load)

        observer.assertValues(
            State.Unknown,
            State.Loading,
            State.Loaded
        )
    }

    @Test
    fun `Command provides initial state`() {

        knot = knot {
            state { initial = State.Loading }
            on<Command.Load> {
                reduceState { command ->
                    command
                        .filter { state == State.Loading }
                        .map { State.Loaded }
                }
            }
        }

        val observer = knot.state.test()
        knot.command.accept(Command.Load)

        observer.assertValues(
            State.Loading,
            State.Loaded
        )
    }

    @Test
    fun `Command provides updated state`() {

        knot = knot {
            state { initial = State.Unknown }
            on<Command.Load> {
                reduceState { command ->
                    command.flatMap<State> {
                        Observable.just(it)
                            .map<State> {
                                if (state == State.Loading) State.Loaded
                                else State.Unknown
                            }
                            .startWith(State.Loading)
                    }
                }
            }
        }

        val observer = knot.state.test()
        knot.command.accept(Command.Load)

        observer.assertValues(
            State.Unknown,
            State.Loading,
            State.Loaded
        )
    }

    private sealed class Command {
        object Load : Command()
    }

    private sealed class State {
        object Unknown : State()
        object Loading : State()
        object Loaded : State()
    }
}
