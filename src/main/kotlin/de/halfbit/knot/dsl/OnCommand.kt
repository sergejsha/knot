package de.halfbit.knot.dsl

import io.reactivex.Observable
import kotlin.reflect.KClass

@KnotDsl
class OnCommand<State : Any, Command : Any, OutCommand : Any>
constructor(
    private val type: KClass<Command>,
    private val commandUpdateStateTransformers: MutableList<TypedCommandUpdateStateTransformer<Command, State>>,
    private val commandToCommandTransformers: MutableList<TypedCommandToCommandTransformer<Command, OutCommand, State>>
) {
    fun updateState(transform: CommandUpdateStateTransform<Command, State>) {
        commandUpdateStateTransformers += TypedCommandUpdateStateTransformer(type, transform)
    }

    fun issueCommand(transform: CommandToCommandTransform<Command, OutCommand, State>) {
        commandToCommandTransformers += TypedCommandToCommandTransformer(type, transform)
    }
}

typealias CommandUpdateStateTransform<Command, State> =
        WithState<State>.(command: Observable<Command>) -> Observable<State>

class TypedCommandUpdateStateTransformer<Command : Any, State : Any>(
    val type: KClass<Command>,
    val transform: CommandUpdateStateTransform<Command, State>
)

typealias CommandToCommandTransform<Command, OutCommand, State> =
        WithState<State>.(command: Observable<Command>) -> Observable<OutCommand>

class TypedCommandToCommandTransformer<Command : Any, OutCommand : Any, State : Any>(
    val type: KClass<Command>,
    val transform: CommandToCommandTransform<Command, OutCommand, State>
)
