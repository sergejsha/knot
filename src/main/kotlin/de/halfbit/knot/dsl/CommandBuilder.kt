package de.halfbit.knot.dsl

import io.reactivex.Observable
import kotlin.reflect.KClass

@KnotDsl
class CommandBuilder<State : Any, Command : Any, OutCommand : Any>
constructor(
    private val type: KClass<Command>,
    private val commandReduceStateTransformers: MutableList<TypedCommandReduceStateTransformer<Command, State>>,
    private val commandToCommandTransformers: MutableList<TypedCommandToCommandTransformer<Command, OutCommand, State>>
) {
    fun reduceState(transform: CommandReduceStateTransform<Command, State>) {
        commandReduceStateTransformers += TypedCommandReduceStateTransformer(type, transform)
    }

    fun toCommand(transform: CommandToCommandTransform<Command, OutCommand, State>) {
        commandToCommandTransformers += TypedCommandToCommandTransformer(type, transform)
    }
}

typealias CommandReduceStateTransform<Command, State> =
        WithState<State>.(command: Observable<Command>) -> Observable<State>

class TypedCommandReduceStateTransformer<Command : Any, State : Any>(
    val type: KClass<Command>,
    val transform: CommandReduceStateTransform<Command, State>
)

typealias CommandToCommandTransform<Command, OutCommand, State> =
        WithState<State>.(command: Observable<Command>) -> Observable<OutCommand>

class TypedCommandToCommandTransformer<Command : Any, OutCommand : Any, State : Any>(
    val type: KClass<Command>,
    val transform: CommandToCommandTransform<Command, OutCommand, State>
)
