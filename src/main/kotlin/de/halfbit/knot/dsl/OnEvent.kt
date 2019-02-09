package de.halfbit.knot.dsl

import io.reactivex.Observable

@KnotDsl
class EventBuilder<State : Any, Event : Any, Command : Any>(
    private val source: Observable<Event>,
    private val onEventUpdateStateTransformers: MutableList<OnEventUpdateStateTransformer<Event, State>>,
    private val onEventToCommandTransformers: MutableList<OnEventToCommandTransformer<Event, Command, State>>
) {
    fun updateState(transform: OnEventUpdateState<Event, State>) {
        onEventUpdateStateTransformers += OnEventUpdateStateTransformer(source, transform)
    }

    fun issueCommand(transform: EventToCommandTransform<Event, Command, State>) {
        onEventToCommandTransformers += OnEventToCommandTransformer(source, transform)
    }
}

typealias OnEventUpdateState<Event, State> =
        WithState<State>.(event: Observable<Event>) -> Observable<State>

class OnEventUpdateStateTransformer<Event : Any, State : Any>(
    val source: Observable<Event>,
    val transform: OnEventUpdateState<Event, State>
)

typealias EventToCommandTransform<Event, Command, State> =
        WithState<State>.(event: Observable<Event>) -> Observable<Command>

class OnEventToCommandTransformer<Event : Any, Command : Any, State : Any>(
    val source: Observable<Event>,
    val transform: EventToCommandTransform<Event, Command, State>
)
