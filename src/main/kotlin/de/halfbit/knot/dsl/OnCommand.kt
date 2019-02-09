package de.halfbit.knot.dsl

import io.reactivex.Observable
import kotlin.reflect.KClass

@KnotDsl
class OnCommand<State : Any, Command : Any, OutCommand : Any>
constructor(
    private val type: KClass<Command>,
    private val onCommandUpdateStateTransformers: MutableList<OnCommandUpdateStateTransformer<Command, State>>,
    private val onCommandToCommandTransformers: MutableList<TypedCommandToCommandTransformer<Command, OutCommand, State>>
) {
    fun updateState(transform: OnCommandUpdateState<Command, State>) {
        onCommandUpdateStateTransformers += OnCommandUpdateStateTransformer(type, transform)
    }

    fun issueCommand(transform: OnCommandIssueCommand<Command, OutCommand, State>) {
        onCommandToCommandTransformers += TypedCommandToCommandTransformer(type, transform)
    }
}

typealias OnCommandUpdateState<Command, State> =
        WithState<State>.(command: Observable<Command>) -> Observable<State>

class OnCommandUpdateStateTransformer<Command : Any, State : Any>(
    val type: KClass<Command>,
    val transform: OnCommandUpdateState<Command, State>
)

typealias OnCommandIssueCommand<Command, OutCommand, State> =
        WithState<State>.(command: Observable<Command>) -> Observable<OutCommand>

class TypedCommandToCommandTransformer<Command : Any, OutCommand : Any, State : Any>(
    val type: KClass<Command>,
    val transform: OnCommandIssueCommand<Command, OutCommand, State>
)
