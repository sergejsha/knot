package de.halfbit.knot.single

import de.halfbit.knot.Knot
import io.reactivex.Observable
import org.junit.Test

class CommandReduceStateTest {

    private lateinit var knot: Knot<State, LoadCommand>

    @Test
    fun `onCommand - reduceState`() {

        knot = singleKnot {
            state { initial = State.Unknown }
            on<LoadCommand> {
                reduceState { command ->
                    command
                        .switchMap {
                            Observable.just(it)
                                .map<State> { State.Loaded }
                                .startWith(State.Loading)
                        }
                }
            }
        }

        val observer = knot.state.test()
        knot.command.accept(LoadCommand)

        observer.assertValues(
            State.Unknown,
            State.Loading,
            State.Loaded
        )
    }

    object LoadCommand

    private sealed class State {
        object Unknown : State()
        object Loading : State()
        object Loaded : State()
    }
}
