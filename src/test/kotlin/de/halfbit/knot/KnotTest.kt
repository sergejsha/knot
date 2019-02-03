package de.halfbit.knot

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeast
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Observable
import org.junit.Test

class KnotTest {

    @Test(expected = IllegalStateException::class)
    fun `DSL builder checks state`() {
        createKnot<State, Command> { }
    }

    @Test
    fun `DSL builder creates Knot`() {
        val knot = createKnot<State, Command> {
            state { initial = State() }
        }
        assertThat(knot).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'state'`() {
        val knot = createKnot<State, Command> {
            state { initial = State() }
        }
        assertThat(knot.state).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'command'`() {
        val knot = createKnot<State, Command> {
            state { initial = State() }
        }
        assertThat(knot.command).isNotNull()
    }

    @Test
    fun `Knot dispatches initial state`() {
        val state = State()
        val knot = createKnot<State, Command> {
            state { initial = state }
        }
        val observer = knot.state.test()
        observer.assertValue(state)
    }

    @Test
    fun `CommandReducer reduces state`() {
        val state = State()
        val stateReduced = state.copy(loaderState = LoaderState.Loading)
        val action = LoadCommandReducer.Action.Loading
        val loadCommandReducer = mock<LoadCommandReducer> {
            on { operator }.thenReturn(Operator(type = Command.Load::class, autoCancelable = false))
            on { onCommand(any(), any()) }.thenReturn(Observable.just(action))
            on { reduce(any(), any()) }.thenReturn(stateReduced)
        }

        val knot = createKnot<State, Command> {
            state { initial = state }
            commands { reduceWith(loadCommandReducer) }
        }

        knot.command.accept(Command.Load)
        val observer = knot.state.test()

        verify(loadCommandReducer, atLeast(1)).operator
        verify(loadCommandReducer).onCommand(Command.Load, state)
        verify(loadCommandReducer).reduce(action, state)
        observer.assertValue(stateReduced)
    }

    @Test
    fun `EventReducer reduces state`() {
        val event = "test"
        val state = State()
        val stateReduced = state.copy(content = event)
        val action = ContentChangedEventReducer.UpdateContentAction(event)
        val contentChangedEventReducer = mock<ContentChangedEventReducer> {
            on { observeEvent() }.thenReturn(Observable.just(event))
            on { onEvent(any(), any()) }.thenReturn(Observable.just(action))
            on { reduce(any(), any()) }.thenReturn(stateReduced)
        }

        val knot = createKnot<State, Command> {
            state { initial = state }
            events { reduceWith(contentChangedEventReducer) }
        }

        verify(contentChangedEventReducer).observeEvent()

        val observer = knot.state.test()

        verify(contentChangedEventReducer).onEvent(event, state)
        verify(contentChangedEventReducer).reduce(action, state)
        observer.assertValue(stateReduced)
    }

    @Test
    fun `EventTransformer transforms event into command`() {
        val state = State()
        val mediaRefreshedEventTransformer = mock<MediaRefreshedEventTransformer> {
            on { transform() }.thenReturn(Observable.just(Command.Load))
        }
        val loadCommandReducer = mock<LoadCommandReducer> {
            on { operator }.thenReturn(Operator(type = Command.Load::class, autoCancelable = false))
            on { onCommand(any(), any()) }.thenReturn(Observable.never())
        }

        createKnot<State, Command> {
            state { initial = state }
            events { transformWith(mediaRefreshedEventTransformer) }
            commands { reduceWith(loadCommandReducer) }
        }

        verify(mediaRefreshedEventTransformer).transform()
        verify(loadCommandReducer, atLeast(1)).operator
        verify(loadCommandReducer).onCommand(Command.Load, state)
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

private interface LoadCommandReducer
    : CommandReducer<Command.Load, LoadCommandReducer.Action, State> {
    sealed class Action {
        object Loading : Action()
        object Loaded : Action()
        class Failed(error: Throwable) : Action()
    }
}

private interface ContentChangedEventReducer
    : EventReducer<String, ContentChangedEventReducer.UpdateContentAction, State> {
    class UpdateContentAction(val content: String)
}

private interface MediaRefreshedEventTransformer
    : EventTransformer<Command>