package de.halfbit.knot.dsl

import io.reactivex.Observable

@KnotDsl
class EventBuilder<State : Any, Event : Any, Command : Any>(
    private val source: Observable<Event>,
    private val eventUpdateStateTransformers: MutableList<SourcedEventUpdateStateTransformer<Event, State>>,
    private val eventToCommandTransformers: MutableList<SourcedEventToCommandTransformer<Event, Command, State>>
) {
    fun updateState(transform: EventUpdateStateTransform<Event, State>) {
        eventUpdateStateTransformers += SourcedEventUpdateStateTransformer(source, transform)
    }

    fun issueCommand(transform: EventToCommandTransform<Event, Command, State>) {
        eventToCommandTransformers += SourcedEventToCommandTransformer(source, transform)
    }
}

typealias EventUpdateStateTransform<Event, State> =
        WithState<State>.(event: Observable<Event>) -> Observable<State>

class SourcedEventUpdateStateTransformer<Event : Any, State : Any>(
    val source: Observable<Event>,
    val transform: EventUpdateStateTransform<Event, State>
)

typealias EventToCommandTransform<Event, Command, State> =
        WithState<State>.(event: Observable<Event>) -> Observable<Command>

class SourcedEventToCommandTransformer<Event : Any, Command : Any, State : Any>(
    val source: Observable<Event>,
    val transform: EventToCommandTransform<Event, Command, State>
)
