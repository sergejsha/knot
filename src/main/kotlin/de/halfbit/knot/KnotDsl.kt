package de.halfbit.knot

@DslMarker
annotation class KnotDsl

@KnotDsl
class KnotBuilder<State : Any, Command : Any>
internal constructor() {

    private var initialState: State? = null
    private var commandReducers = mutableListOf<CommandReducer<Command, Any, State>>()
    private var eventReducers = mutableListOf<EventReducer<Any, Any, State>>()
    private var eventTransformers = mutableListOf<EventTransformer<Command>>()

    fun state(block: StateBuilder<State>.() -> Unit) {
        StateBuilder<State>().also(block).also { initialState = it.initial }
    }

    fun commands(block: CommandsBuilder<State, Command>.() -> Unit) {
        CommandsBuilder(commandReducers).also(block)
    }

    fun events(block: EventsBuilder<State, Command>.() -> Unit) {
        EventsBuilder(eventReducers, eventTransformers).also(block)
    }

    fun build(): Knot<State, Command> {
        val initialState = checkNotNull(initialState) { "state { initial } must be set" }
        return DefaultKnot(
            initialState = initialState,
            commandReducers = commandReducers,
            eventReducers = eventReducers,
            eventTransformers = eventTransformers
        )
    }
}

@KnotDsl
class CommandsBuilder<State : Any, Command : Any>
internal constructor(
    private val commandReducers: MutableList<CommandReducer<Command, Any, State>>
) {
    fun reduceWith(commandReducer: CommandReducer<out Command, out Any, State>) {
        @Suppress("UNCHECKED_CAST")
        commandReducers.add(commandReducer as CommandReducer<Command, Any, State>)
    }
}

@KnotDsl
class EventsBuilder<State : Any, Command : Any>
internal constructor(
    private val eventReducers: MutableList<EventReducer<Any, Any, State>>,
    private val eventTransformers: MutableList<EventTransformer<Command>>
) {
    fun reduceWith(eventReducer: EventReducer<out Any, out Any, State>) {
        @Suppress("UNCHECKED_CAST")
        eventReducers.add(eventReducer as EventReducer<Any, Any, State>)
    }

    fun transformWith(eventTransformer: EventTransformer<Command>) {
        eventTransformers.add(eventTransformer)
    }
}

@KnotDsl
class StateBuilder<State>
internal constructor(
    var initial: State? = null
)

fun <State : Any, Command : Any> createKnot(
    block: KnotBuilder<State, Command>.() -> Unit
): Knot<State, Command> =
    KnotBuilder<State, Command>()
        .also(block)
        .build()
