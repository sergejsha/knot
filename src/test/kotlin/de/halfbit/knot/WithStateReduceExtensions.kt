package de.halfbit.knot

import de.halfbit.knot.dsl.Reducer
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class WithStateReduceExtensions {

    object LoadCommand
    sealed class State {
        object Unknown : State()
        object Loaded : State()
        object Error : State()
    }

    private lateinit var knot: Knot<State, LoadCommand>

    @Test
    fun `reduceState is available in onCommand`() {

        knot = tieKnot {
            state { initial = State.Unknown }
            on<LoadCommand> {
                updateState { it.reduceState { State.Loaded } }
            }
        }

        val observer = knot.state.test()
        knot.command.accept(LoadCommand)

        observer.assertValues(
            State.Unknown,
            State.Loaded
        )
    }

    @Test
    fun `reduceStateOnError is available in onCommand`() {

        val error = IllegalStateException("Kaboom")
        knot = tieKnot {
            state { initial = State.Unknown }
            on<LoadCommand> {
                updateState { command ->
                    command
                        .map<Reducer<State>> { throw error }
                        .reduceStateOnError { State.Error }
                }
            }
        }

        val observer = knot.state.test()
        knot.command.accept(LoadCommand)

        observer.assertValues(
            State.Unknown,
            State.Error
        )
    }

    @Test
    fun `reduceState is available in onEvent`() {

        val event: PublishSubject<Unit> = PublishSubject.create()

        knot = tieKnot {
            state { initial = State.Unknown }
            on(event) {
                updateState { it.reduceState { State.Loaded } }
            }
        }

        val observer = knot.state.test()
        event.onNext(Unit)

        observer.assertValues(
            State.Unknown,
            State.Loaded
        )
    }

    @Test
    fun `reduceStateOnError is available in onEvent`() {

        val event: PublishSubject<Unit> = PublishSubject.create()
        val error = IllegalStateException("Kaboom")
        knot = tieKnot {
            state { initial = State.Unknown }
            on(event) {
                updateState { event ->
                    event
                        .map<Reducer<State>> { throw error }
                        .reduceStateOnError { State.Error }
                }
            }
        }

        val observer = knot.state.test()
        event.onNext(Unit)

        observer.assertValues(
            State.Unknown,
            State.Error
        )
    }

}