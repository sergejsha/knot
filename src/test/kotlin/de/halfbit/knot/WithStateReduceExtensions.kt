package de.halfbit.knot

import de.halfbit.knot.dsl.Reducer
import io.reactivex.Completable
import io.reactivex.Observable
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
                updateState { it.mapReduceState { State.Loaded } }
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
    fun `reduceState is available in onEvent`() {

        val event: PublishSubject<Unit> = PublishSubject.create()
        knot = tieKnot {
            state { initial = State.Unknown }
            on(event) {
                updateState { it.mapReduceState { State.Loaded } }
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
    fun `reduceStateOnError is available in onCommand`() {

        val error = IllegalStateException("Kaboom")
        knot = tieKnot {
            state { initial = State.Unknown }
            on<LoadCommand> {
                updateState { command ->
                    command
                        .map<Reducer<State>> { throw error }
                        .onErrorReduceState { State.Error }
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
    fun `reduceStateOnError is available in onEvent`() {

        val event: PublishSubject<Unit> = PublishSubject.create()
        val error = IllegalStateException("Kaboom")
        knot = tieKnot {
            state { initial = State.Unknown }
            on(event) {
                updateState { event ->
                    event
                        .map<Reducer<State>> { throw error }
                        .onErrorReduceState { State.Error }
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

    @Test
    fun `andThenReduceState is available in onCommand`() {

        knot = tieKnot {
            state { initial = State.Unknown }
            on<LoadCommand> {
                updateState {
                    it.switchMap<Reducer<State>> {
                        Completable.complete()
                            .andThenReduceState { State.Loaded }
                    }
                }
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
    fun `andThenReduceState is available in onEvent`() {

        val event: PublishSubject<Unit> = PublishSubject.create()
        knot = tieKnot {
            state { initial = State.Unknown }
            on(event) {
                updateState {
                    it.switchMap<Reducer<State>> {
                        Completable.complete()
                            .andThenReduceState { State.Loaded }
                    }
                }
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
    fun `switchMapReduceState is available in onCommand`() {

        knot = tieKnot {
            state { initial = State.Unknown }
            on<LoadCommand> {
                updateState {
                    it.switchMapReduceState {
                        Observable
                            .just<Reducer<State>>(reduce { State.Loaded })
                    }
                }
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
    fun `switchMapReduceState is available in onEvent`() {

        val event: PublishSubject<Unit> = PublishSubject.create()
        knot = tieKnot {
            state { initial = State.Unknown }
            on(event) {
                updateState {
                    it.switchMapReduceState {
                        Observable
                            .just<Reducer<State>>(reduce { State.Loaded })
                    }
                }
            }
        }

        val observer = knot.state.test()
        event.onNext(Unit)

        observer.assertValues(
            State.Unknown,
            State.Loaded
        )
    }

}