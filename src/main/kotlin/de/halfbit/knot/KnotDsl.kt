package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler

fun <State : Any, Change : Any> knot(
    block: KnotBuilder<State, Change>.() -> Unit
): Knot<State, Change> =
    KnotBuilder<State, Change>()
        .also(block)
        .build()

@DslMarker
annotation class KnotDsl

@KnotDsl
class KnotBuilder<State : Any, Change : Any>
internal constructor() {
    private var initialState: State? = null
    private var reducer: Reducer<State, Change>? = null
    private var observeOn: Scheduler? = null
    private var reduceOn: Scheduler? = null
    private val eventTransformers = mutableListOf<EventTransformer<Change>>()

    fun state(block: OnState<State, Change>.() -> Unit) {
        OnState<State, Change>()
            .also {
                block(it)
                initialState = it.initial
                reducer = it.reducer
                observeOn = it.observeOn
                reduceOn = it.reduceOn
            }
    }

    fun onEvent(transformer: EventTransformer<Change>) {
        eventTransformers += transformer
    }

    fun build(): Knot<State, Change> = DefaultKnot(
        initialState = checkNotNull(initialState) { "knot { state { initialState } } must be set" },
        reducer = checkNotNull(reducer) { "knot { state { reducer } } must be set" },
        observeOn = observeOn,
        reduceOn = reduceOn,
        eventTransformers = eventTransformers
    )
}

@KnotDsl
class OnState<State : Any, Change : Any>
internal constructor() {
    internal var reducer: Reducer<State, Change>? = null

    var initial: State? = null
    var observeOn: Scheduler? = null
    var reduceOn: Scheduler? = null

    fun reduce(reducer: Reducer<State, Change>) {
        this.reducer = reducer
    }
}

typealias Reducer<State, Change> = (change: Change, state: State) -> Effect<State, Change>
typealias EventTransformer<Change> = () -> Observable<Change>
