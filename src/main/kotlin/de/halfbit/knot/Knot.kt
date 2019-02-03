package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

internal class DefaultKnot<State : Any, Command : Any>(
    initialState: State,
    commandReducers: List<CommandReducer<Command, Any, State>>,
    eventReducers: List<EventReducer<Any, Any, State>>,
    eventTransformers: List<EventTransformer<Command>>,
    private val disposables: CompositeDisposable = CompositeDisposable()
) : Knot<State, Command> {

    private var stateValue = AtomicReference(initialState)
    private val _command = PublishSubject.create<Command>()
    private val _state = Observable
        .merge(observablesFrom(commandReducers, eventReducers))
        .serialize()
        .map { it.reducer.reduce(it.action, stateValue.get()) }
        .startWith(initialState)
        .distinctUntilChanged()
        .doOnNext { stateValue.set(it) }
        .replay(1)
        .also { disposables += it.connect() }

    override val state: Observable<State> = _state
    override val command: Consumer<Command> = Consumer { _command.onNext(it) }

    init {
        disposables += Observable
            .merge(mutableListOf<Observable<Command>>().also { list ->
                for (eventTransformer in eventTransformers) {
                    list += eventTransformer.transform()
                }
            })
            .subscribe { _command.onNext(it) }
    }

    private fun observablesFrom(
        commandReducers: List<CommandReducer<Command, Any, State>>,
        eventReducers: List<EventReducer<Any, Any, State>>
    ): List<Observable<ActionReducer<Any, State>>> =
        mutableListOf<Observable<ActionReducer<Any, State>>>().also { list ->
            for (commandReducer in commandReducers) {
                list += _command
                    .ofType(commandReducer.operator.type.javaObjectType)
                    .mapCommandReducer(commandReducer)
                    .map { ActionReducer(it, commandReducer) }
            }
            for (eventReducer in eventReducers) {
                list += eventReducer.observeEvent()
                    .flatMap { eventReducer.onEvent(it, stateValue.get()) }
                    .map { ActionReducer(it, eventReducer) }
            }
        }

    private fun Observable<Command>.mapCommandReducer(reducer: CommandReducer<Command, Any, State>): Observable<Any> =
        if (reducer.operator.autoCancelable) this.switchMap { reducer.onCommand(it, stateValue.get()) }
        else this.flatMap { reducer.onCommand(it, stateValue.get()) }

    private class ActionReducer<Action : Any, State : Any>(
        val action: Action,
        val reducer: Reducer<Action, State>
    )
}

interface Knot<State : Any, Command : Any> {
    val state: Observable<State>
    val command: Consumer<Command>
}

interface Reducer<Action : Any, State : Any> {
    fun reduce(action: Action, state: State): State
}

interface EventTransformer<Command : Any> {
    fun transform(): Observable<Command>
}

interface EventReducer<Event : Any, Action : Any, State : Any> : Reducer<Action, State> {
    fun observeEvent(): Observable<Event>
    fun onEvent(event: Event, state: State): Observable<Action>
}

interface CommandReducer<Command : Any, Action : Any, State : Any> : Reducer<Action, State> {
    val operator: Operator<Command>
    fun onCommand(command: Command, state: State): Observable<Action>
}

class Operator<Command : Any>(
    val type: KClass<Command>,
    val autoCancelable: Boolean
)
