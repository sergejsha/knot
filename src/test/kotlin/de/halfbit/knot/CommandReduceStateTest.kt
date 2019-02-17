package de.halfbit.knot

import de.halfbit.knot.dsl.Reducer
import io.reactivex.Observable
import org.junit.Test

class CommandReduceStateTest {

    private lateinit var knot: Knot<State, Command>

    @Test
    fun `Command reduces state`() {

        knot = tieKnot {
            state { initial = State.Unknown }
            on<Command.Load> {
                updateState { command ->
                    command
                        .flatMap<Reducer<State>> {
                            Observable.just(it)
                                .map { reduceState { State.Loaded } }
                                .startWith(reduceState { State.Loading })
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

        knot = tieKnot {
            state { initial = State.Loading }
            on<Command.Load> {
                updateState { command ->
                    command
                        .filter { state == State.Loading }
                        .map<Reducer<State>> { reduceState { State.Loaded } }
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

        knot = tieKnot {
            state { initial = State.Unknown }
            on<Command.Load> {
                updateState { command ->
                    command.flatMap<Reducer<State>> {
                        Observable.just(it)
                            .map {
                                reduceState {
                                    if (state == State.Loading) State.Loaded
                                    else State.Unknown
                                }
                            }
                            .startWith { State.Loading }
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
    fun `Multiple command handlers are called in order`() {

        knot = tieKnot {
            state { initial = State.Unknown }
            on<Command.Load> {
                updateState { command ->
                    command
                        .map<Reducer<State>> {
                            reduceState { State.Loading }
                        }
                }
            }
            on<Command.Load> {
                updateState { command ->
                    command
                        .map<Reducer<State>> {
                            reduceState { State.Loaded }
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
    fun `Multiple state reducers for same command are called in order`() {

        knot = tieKnot {
            state { initial = State.Unknown }
            on<Command.Load> {
                updateState { command ->
                    command
                        .map<Reducer<State>> {
                            reduceState { State.Loading }
                        }
                }
                updateState { command ->
                    command
                        .map<Reducer<State>> {
                            reduceState { State.Loaded }
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
