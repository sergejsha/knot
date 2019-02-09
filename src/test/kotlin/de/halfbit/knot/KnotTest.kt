package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import io.reactivex.Observable
import org.junit.Test

class KnotTest {

    @Test(expected = IllegalStateException::class)
    fun `DSL builder checks state`() {
        knot<State, Command> { }
    }

    @Test
    fun `DSL builder creates Knot`() {
        val knot = knot<State, Command> {
            state { initial = State() }
        }
        assertThat(knot).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'state'`() {
        val knot = knot<State, Command> {
            state { initial = State() }
        }
        assertThat(knot.state).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'command'`() {
        val knot = knot<State, Command> {
            state { initial = State() }
        }
        assertThat(knot.command).isNotNull()
    }

    @Test
    fun `Knot dispatches initial state`() {
        val state = State()
        val knot = knot<State, Command> {
            state { initial = state }
        }
        val observer = knot.state.test()
        observer.assertValue(state)
    }

    @Test
    fun `Test onCommand() - toState()`() {
        val knot = knot<State, Command> {
            state { initial = State() }
            onCommand<Command.Load> {
                toState { command ->
                    command
                        .filter { state.loaderState != LoaderState.Loading }
                        .flatMap<State> {
                            Observable.fromCallable { }
                                .map<State> { state.copy(loaderState = LoaderState.Loaded) }
                                .startWith(state.copy(loaderState = LoaderState.Loading))
                        }
                }
            }
        }
        val observer = knot.state.test()
        println(observer.values())
    }
}

private data class State(
    val loaderState: LoaderState = LoaderState.Unknown(),
    val content: String? = null
)

private sealed class Command {
    object Load : Command()
}

private sealed class LoaderState {
    class Unknown(error: Throwable? = null) : LoaderState()
    object Loading : LoaderState()
    object Loaded : LoaderState()
}
