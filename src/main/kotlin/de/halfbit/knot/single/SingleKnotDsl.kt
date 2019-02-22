package de.halfbit.knot.single

import de.halfbit.knot.Knot
import io.reactivex.Observable
import kotlin.reflect.KClass

@DslMarker
annotation class KnotDslMaker

@KnotDslMaker
class KnotBuilder<State : Any, Command : Any> {

    private var initialState: State? = null
    private val eventToCommandTransformers: MutableList<SourcedEventToCommandTransformer<Any, Command>> =
        mutableListOf()

    @PublishedApi
    internal val commandToStateTransformers: MutableList<CommandToStateTransformer<Command, State>> = arrayListOf()

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
                as MutableList<CommandToStateTransformer<C, State>>
    ).also(init)

    @Suppress("UNCHECKED_CAST")
    fun <Event : Any> onEvent(
        source: Observable<Event>,
        init: OnEvent<Command, Event>.() -> Unit
    ): OnEvent<Command, Event> = OnEvent<Command, Event>(
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
    private val stateTransformers: MutableList<CommandToStateTransformer<Command, State>>
) {
    fun reduceState(transformer: CommandToStateTransformer<Command, State>) {
        stateTransformers += TypedCommandToStateTransformer(type, transformer)
    }
}

@KnotDslMaker
class OnState<State : Any>
internal constructor() {
    internal var initial: State? = null
}

@KnotDslMaker
class OnEvent<Command : Any, Event : Any>
internal constructor(
    private val commandTransformers: MutableList<SourcedEventToCommandTransformer<Any, Command>>,
    private val source: Observable<Any>
) {
    fun issueCommand(transformer: EventToCommandTransformer<in Event, Command>) {
        commandTransformers += SourcedEventToCommandTransformer(source, transformer)
    }
}

typealias CommandToStateTransformer<Command, State> = (command: Observable<Command>) -> Observable<State>
typealias EventToCommandTransformer<Event, Command> = (event: Observable<Event>) -> Observable<Command>

class SourcedEventToCommandTransformer<Event : Any, Command : Any>(
    val source: Observable<Event>,
    val transformer: EventToCommandTransformer<Event, Command>
)

private class TypedCommandToStateTransformer<Command : Any, State : Any, C : Command>(
    private val type: KClass<C>,
    private val transformer: CommandToStateTransformer<C, State>
) : CommandToStateTransformer<Command, State> {
    override fun invoke(command: Observable<Command>): Observable<State> =
        command.ofType(type.javaObjectType).compose(transformer)
}
