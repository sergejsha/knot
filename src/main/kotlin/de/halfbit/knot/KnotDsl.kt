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
    val commandReduceStateTransformers = mutableListOf<TypedCommandReduceStateTransformer<Command, State>>()
    val eventReduceStateTransformers = mutableListOf<SourcedEventReduceStateTransformer<*, State>>()
    val eventToCommandTransformers = mutableListOf<SourcedEventToCommandTransformer<*, Command, State>>()

    fun build(): Knot<State, Command> = DefaultKnot(
        checkNotNull(initialState) { "state { initial } must be set" },
        commandReduceStateTransformers,
        eventReduceStateTransformers,
        eventToCommandTransformers
    )

    @Suppress("UNCHECKED_CAST")
    inline fun <reified C : Command> on(
        commandBuilderBlock: CommandBuilder<State, C>.() -> Unit
    ) {
        val reducers = mutableListOf<TypedCommandReduceStateTransformer<C, State>>()
        CommandBuilder(C::class, reducers).also(commandBuilderBlock)
        commandReduceStateTransformers += reducers as List<TypedCommandReduceStateTransformer<Command, State>>
    }

    inline fun <Event : Any> on(
        source: Observable<Event>, eventBuilder: EventBuilder<State, Event, Command>.() -> Unit
    ) {
        val transformers1 = mutableListOf<SourcedEventReduceStateTransformer<Event, State>>()
        val transformers2 = mutableListOf<SourcedEventToCommandTransformer<Event, Command, State>>()
        EventBuilder(source, transformers1, transformers2).also(eventBuilder)
        eventReduceStateTransformers += transformers1 as List<SourcedEventReduceStateTransformer<*, State>>
        eventToCommandTransformers += transformers2 as List<SourcedEventToCommandTransformer<*, Command, State>>
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
    private val transformers: MutableList<TypedCommandReduceStateTransformer<Command, State>>
) {
    fun reduceState(reducer: CommandReduceStateTransform<Command, State>) {
        transformers.add(TypedCommandReduceStateTransformer(type, reducer))
    }
}

typealias CommandReduceStateTransform<Command, State> =
        WithState<State>.(command: Observable<Command>) -> Observable<State>

class TypedCommandReduceStateTransformer<Command : Any, State : Any>(
    val type: KClass<Command>,
    val transform: CommandReduceStateTransform<Command, State>
)

@KnotDsl
class EventBuilder<State : Any, Event : Any, Command : Any>(
    private val source: Observable<Event>,
    private val eventReduceStateTransformers: MutableList<SourcedEventReduceStateTransformer<Event, State>>,
    private val eventToCommandTransformers: MutableList<SourcedEventToCommandTransformer<Event, Command, State>>
) {
    fun reduceState(transform: EventReduceStateTransform<Event, State>) {
        eventReduceStateTransformers += SourcedEventReduceStateTransformer(source, transform)
    }

    fun toCommand(transform: EventToCommandTransform<Event, Command, State>) {
        eventToCommandTransformers += SourcedEventToCommandTransformer(source, transform)
    }
}

typealias EventReduceStateTransform<Event, State> =
        WithState<State>.(event: Observable<Event>) -> Observable<State>

interface WithState<State : Any> {
    val state: State
}

class SourcedEventReduceStateTransformer<Event : Any, State : Any>(
    val source: Observable<Event>,
    val transform: EventReduceStateTransform<Event, State>
)

typealias EventToCommandTransform<Event, Command, State> =
        WithState<State>.(event: Observable<Event>) -> Observable<Command>

class SourcedEventToCommandTransformer<Event : Any, Command : Any, State : Any>(
    val source: Observable<Event>,
    val transform: EventToCommandTransform<Event, Command, State>
)
