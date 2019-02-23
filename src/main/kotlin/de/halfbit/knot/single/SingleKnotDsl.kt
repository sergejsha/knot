package de.halfbit.knot.single

import de.halfbit.knot.Knot
import io.reactivex.Observable
import kotlin.reflect.KClass

@DslMarker
annotation class KnotDslMaker

@KnotDslMaker
class KnotBuilder<State : Any, Command : Any> {

    private var initialState: State? = null
    private val eventToCommandTransformers
            : MutableList<SourcedEventToCommandTransformer<Any, Command, State>> = mutableListOf()

    @PublishedApi
    internal val commandToStateTransformers
            : MutableList<TypedCommandToStateTransformer<Command, State>> = mutableListOf()

    fun state(
        init: OnState<State>.() -> Unit
    ): OnState<State> = OnState<State>()
        .also {
            it.init()
            initialState = it.initial
        }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified C : Command> on(
        init: OnCommand<State, C>.() -> Unit
    ): OnCommand<State, C> = OnCommand(
        type = C::class,
        stateTransformers = commandToStateTransformers
                as MutableList<TypedCommandToStateTransformer<C, State>>
    ).also(init)

    @Suppress("UNCHECKED_CAST")
    fun <Event : Any> onEvent(
        source: Observable<Event>,
        init: OnEvent<Command, Event, State>.() -> Unit
    ): OnEvent<Command, Event, State> = OnEvent<Command, Event, State>(
        commandTransformers = eventToCommandTransformers,
        source = source as Observable<Any>
    ).also(init)

    fun build(): Knot<State, Command> = SingleKnot(
        initialState = checkNotNull(initialState) { "state { initial } is required" },
        commandToStateTransformers = commandToStateTransformers,
        eventToCommandTransformers = eventToCommandTransformers
    )
}

@KnotDslMaker
class OnCommand<State : Any, Command : Any>
constructor(
    private val type: KClass<Command>,
    private val stateTransformers: MutableList<TypedCommandToStateTransformer<Command, State>>
) {
    fun reduceState(transformer: CommandToStateTransformer<Command, State>) {
        stateTransformers += TypedCommandToStateTransformer(type, transformer)
    }
}

@KnotDslMaker
class OnState<State : Any>
internal constructor() {
    var initial: State? = null
}

@KnotDslMaker
class OnEvent<Command : Any, Event : Any, State : Any>
internal constructor(
    private val commandTransformers: MutableList<SourcedEventToCommandTransformer<Any, Command, State>>,
    private val source: Observable<Any>
) {
    fun issueCommand(transformer: EventToCommandTransformer<in Event, Command, State>) {
        commandTransformers += SourcedEventToCommandTransformer(source, transformer)
    }
}

interface WithState<State : Any> {
    val state: State
}

typealias CommandToStateTransformer<Command, State> =
        WithState<State>.(command: Observable<Command>) -> Observable<State>

typealias EventToCommandTransformer<Event, Command, State> =
        WithState<State>.(event: Observable<Event>) -> Observable<Command>

class SourcedEventToCommandTransformer<Event : Any, Command : Any, State : Any>(
    val source: Observable<Event>,
    val transformer: EventToCommandTransformer<Event, Command, State>
)

class TypedCommandToStateTransformer<Command : Any, State : Any>(
    val type: KClass<Command>,
    val transformer: CommandToStateTransformer<Command, State>
)
