package de.halfbit.knot.dsl

import de.halfbit.knot.DefaultKnot
import de.halfbit.knot.Knot
import io.reactivex.Observable

@DslMarker
annotation class KnotDsl

interface WithState<State : Any> {
    val state: State
}

@KnotDsl
class KnotBuilder<State : Any, Command : Any> {

    private var initialState: State? = null
    val commandUpdateStateTransformers = mutableListOf<TypedCommandUpdateStateTransformer<Command, State>>()
    val commandToCommandTransformers = mutableListOf<TypedCommandToCommandTransformer<Command, Command, State>>()
    val eventUpdateStateTransformers = mutableListOf<SourcedEventUpdateStateTransformer<*, State>>()
    val eventToCommandTransformers = mutableListOf<SourcedEventToCommandTransformer<*, Command, State>>()

    fun build(): Knot<State, Command> = DefaultKnot(
        checkNotNull(initialState) { "state { initial } must be set" },
        commandUpdateStateTransformers,
        commandToCommandTransformers,
        eventUpdateStateTransformers,
        eventToCommandTransformers
    )

    @Suppress("UNCHECKED_CAST")
    inline fun <reified C : Command> on(
        onCommand: OnCommand<State, C, Command>.() -> Unit
    ): OnCommand<State, C, Command> {
        return OnCommand(
            C::class,
            commandUpdateStateTransformers as MutableList<TypedCommandUpdateStateTransformer<C, State>>,
            commandToCommandTransformers as MutableList<TypedCommandToCommandTransformer<C, Command, State>>
        ).also(onCommand)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <Event : Any> on(
        source: Observable<Event>, eventBuilder: EventBuilder<State, Event, Command>.() -> Unit
    ) {
        EventBuilder(
            source,
            eventUpdateStateTransformers as MutableList<SourcedEventUpdateStateTransformer<Event, State>>,
            eventToCommandTransformers as MutableList<SourcedEventToCommandTransformer<Event, Command, State>>
        ).also(eventBuilder)
    }

    fun state(state: StateBuilder<State>.() -> Unit) {
        StateBuilder<State>()
            .also(state)
            .let { initialState = it.initial }
    }

}
