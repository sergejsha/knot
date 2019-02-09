package de.halfbit.knot

import io.reactivex.Observable
import kotlin.reflect.KClass

fun <State : Any, Command : Any> knot(
    block: KnotBuilder<State, Command>.() -> Unit
): Knot<State, Command> =
    KnotBuilder<State, Command>()
        .also(block)
        .build()

@DslMarker
annotation class KnotDsl

@KnotDsl
class KnotBuilder<State : Any, Command : Any> {

    private var initialState: State? = null
    val commandToStateTransformers = mutableListOf<TypedCommandToStateTransformer<Command, State>>()
    val eventToStateTransformers = mutableListOf<SourcedEventToStateTransformer<*, State>>()
    val eventToCommandTransformers = mutableListOf<SourcedEventToCommandTransformer<*, Command, State>>()

    fun build(): Knot<State, Command> = DefaultKnot(
        checkNotNull(initialState) { "state { initial } must be set" },
        commandToStateTransformers,
        eventToStateTransformers,
        eventToCommandTransformers
    )

    inline fun <reified C : Command> onCommand(
        commandBuilderBlock: CommandBuilder<State, C>.() -> Unit
    ) {
        val reducers = mutableListOf<TypedCommandToStateTransformer<C, State>>()
        CommandBuilder(C::class, reducers).also(commandBuilderBlock)
        commandToStateTransformers += reducers as List<TypedCommandToStateTransformer<Command, State>>
    }

    inline fun <Event : Any> onEvent(
        source: Observable<Event>, eventBuilder: EventBuilder<State, Event, Command>.() -> Unit
    ) {
        val reducers1 = mutableListOf<SourcedEventToStateTransformer<Event, State>>()
        val reducers2 = mutableListOf<SourcedEventToCommandTransformer<Event, Command, State>>()
        EventBuilder(source, reducers1, reducers2).also(eventBuilder)
        eventToStateTransformers += reducers1 as SourcedEventToStateTransformer<*, State>
        eventToCommandTransformers += reducers2 as SourcedEventToCommandTransformer<*, Command, State>
    }

    fun state(state: StateBuilder<State>.() -> Unit) {
        StateBuilder<State>()
            .also(state)
            .let {
                initialState = it.initial
            }
    }

}

@KnotDsl
class StateBuilder<State : Any>
internal constructor() {
    var initial: State? = null
}

@KnotDsl
class CommandBuilder<State : Any, Command : Any>
constructor(
    private val type: KClass<Command>,
    private val transformers: MutableList<TypedCommandToStateTransformer<Command, State>>
) {
    fun toState(reducer: CommandToStateTransform<Command, State>) {
        transformers.add(TypedCommandToStateTransformer(type, reducer))
    }
}

typealias CommandToStateTransform<Command, State> =
        WithState<State>.(command: Observable<Command>) -> Observable<State>

class TypedCommandToStateTransformer<Command : Any, State : Any>(
    val type: KClass<Command>,
    val transform: CommandToStateTransform<Command, State>
)

@KnotDsl
class EventBuilder<State : Any, Event : Any, Command : Any>(
    private val source: Observable<Event>,
    private val eventToStateTransformers: MutableList<SourcedEventToStateTransformer<Event, State>>,
    private val eventToCommandTransformers: MutableList<SourcedEventToCommandTransformer<Event, Command, State>>
) {
    fun toState(transform: EventToStateTransform<Event, State>) {
        eventToStateTransformers += SourcedEventToStateTransformer(source, transform)
    }

    fun toCommand(transform: EventToCommandTransform<Event, Command, State>) {
        eventToCommandTransformers += SourcedEventToCommandTransformer(source, transform)
    }
}

typealias EventToStateTransform<Event, State> =
        WithState<State>.(event: Observable<Event>) -> Observable<State>

interface WithState<State : Any> {
    val state: State
}

class SourcedEventToStateTransformer<Event : Any, State : Any>(
    val source: Observable<Event>,
    val transform: EventToStateTransform<Event, State>
)

typealias EventToCommandTransform<Event, Command, State> =
        WithState<State>.(event: Observable<Event>) -> Observable<Command>

class SourcedEventToCommandTransformer<Event : Any, Command : Any, State : Any>(
    val source: Observable<Event>,
    val transform: EventToCommandTransform<Event, Command, State>
)
