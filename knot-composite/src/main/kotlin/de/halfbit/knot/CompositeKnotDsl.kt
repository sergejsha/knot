package de.halfbit.knot

import io.reactivex.Scheduler

fun <State : Any, Change : Any, Action : Any> compositeKnot(
    block: CompositeKnotBuilder<State, Change, Action>.() -> Unit
): CompositeKnot<State, Change, Action> =
    CompositeKnotBuilder<State, Change, Action>()
        .also(block)
        .build()

@DslMarker
annotation class CompositeKnotDsl

@CompositeKnotDsl
class CompositeKnotBuilder<State : Any, Change : Any, Action : Any>
internal constructor() {

    private var initialState: State? = null
    private var observeOn: Scheduler? = null
    private var reduceOn: Scheduler? = null

    fun state(block: StateBuilder<State>.() -> Unit) {
        StateBuilder<State>()
            .also {
                block(it)
                initialState = it.initial
                observeOn = it.observeOn
                reduceOn = it.reduceOn
            }
    }

    fun build(): CompositeKnot<State, Change, Action> = DefaultCompositeKnot(
        initialState = checkNotNull(initialState) { "compositeKnot { state { initial } } must be set" },
        observeOn = observeOn,
        reduceOn = reduceOn
    )

    @KnotDsl
    class StateBuilder<State : Any>
    internal constructor() {
        var initial: State? = null
        var observeOn: Scheduler? = null
        var reduceOn: Scheduler? = null
    }
}