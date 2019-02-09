package de.halfbit.knot

import de.halfbit.knot.dsl.tieKnot
import org.junit.Test

class CommandToCommandTest {

    private lateinit var knot: Knot<State, Command>

    @Test
    fun `Event can be transformed into command`() {

        knot = tieKnot {
            state { initial = State.Unknown }
            on<Command.Start> {
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
        knot.command.accept(Command.Start)

        observer.assertValues(
            State.Unknown,
            State.Loaded
        )
    }

    private sealed class Command {
        object Start : Command()
        object Load : Command()
    }

    private sealed class State {
        object Unknown : State()
        object Loaded : State()
    }

}