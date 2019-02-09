package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicReference

interface Knot<State : Any, Command : Any> {
    val state: Observable<State>
    val command: Consumer<Command>
    fun dispose()
}

internal class DefaultKnot<State : Any, Command : Any>(
    initialState: State,
    commandToStateTransformers: List<TypedCommandToStateTransformer<Command, State>>,
    eventToStateTransformers: List<SourcedEventToStateTransformer<*, State>>,
    eventToCommandTransformers: List<SourcedEventToCommandTransformer<*, Command, State>>,
    private val disposables: CompositeDisposable = CompositeDisposable()
) : Knot<State, Command> {

    private val stateValue = AtomicReference(initialState)
    private val withState = object : WithState<State> {
        override val state: State get() = stateValue.get()
    }

    private val _command = PublishSubject.create<Command>()
    private val _state = Observable
        .merge(transformers(commandToStateTransformers, eventToStateTransformers))
        .serialize()
        .startWith(initialState)
        .distinctUntilChanged()
        .doOnNext { stateValue.set(it) }
        .replay(1)
        .also { disposables += it.connect() }

    override val state: Observable<State> = _state
    override val command: Consumer<Command> = Consumer { _command.onNext(it) }

    init {
        disposables += Observable
            .merge(
                mutableListOf<Observable<Command>>().also { list ->
                    for (transformer in eventToCommandTransformers) {
                        list += transformer.source
                            .compose {
                                val transform = transformer.transform as EventToCommandTransform<*, Command, State>
                                transform(withState, it)
                            }
                    }
                }
            )
            .subscribe { _command.onNext(it) }
    }

    private fun transformers(
        commandToStateTransformers: List<TypedCommandToStateTransformer<Command, State>>,
        eventToStateTransformers: List<SourcedEventToStateTransformer<*, State>>
    ): List<Observable<State>> =
        mutableListOf<Observable<State>>().also { list ->
            for (transformer in commandToStateTransformers) {
                list += _command
                    .ofType(transformer.type.javaObjectType)
                    .compose<State> { transformer.transform(withState, it) }
            }
            for (transformer in eventToStateTransformers) {
                list += transformer.source
                    .compose<State> {
                        val transform = transformer.transform as EventToStateTransform<*, State>
                        transform(withState, it)
                    }
            }
        }

    override fun dispose() {
        disposables.clear()
    }
}

/*
lateinit var eventSource: EventSource
lateinit var userProfileRepository: AnotherEventSource

val knot = knot<State, Command> {

    state {
        initial = State()
    }

    onCommand<Command.FetchPaymentMethod> {
        toState { fetchPaymentMethod ->
            fetchPaymentMethod
                .filter { !state.fetching }
                .map { state.copy() }
        }
    }

    onEvent(eventSource.events) {
        toCommand {
            it.map { Command.FetchPaymentMethod }
        }
    }

    onEvent(userProfileRepository.userProfile) {

        toCommand { userProfile ->
            userProfile
                .filter { it.isRegistered }
                .map { Command.FetchPaymentMethod }
        }

        toCommand { userProfile ->
            userProfile
                .filter { !it.isRegistered }
                .map { Command.InvalidateCustomer }
        }

        toState { userProfile ->
            userProfile
                .filter { it.isRegistered }
                .map { state.copy(fetching = false) }
        }

    }

}

interface EventSource {
    val events: Observable<EventType>
}

interface AnotherEventSource {
    val userProfile: Observable<UserProfile>
}

data class State(
    val fetching: Boolean = false
)

sealed class Command {
    object FetchPaymentMethod : Command()
    object InvalidateCustomer : Command()
}

class EventType
class UserProfile(val isRegistered: Boolean = false)
*/