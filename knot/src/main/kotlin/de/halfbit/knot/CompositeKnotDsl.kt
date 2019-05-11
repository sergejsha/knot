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

    /** A section for [State] related declarations. */
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

    @CompositeKnotDsl
    class StateBuilder<State : Any>
    internal constructor() {

        /** Mandatory initial [State] of the [CompositeKnot]. */
        var initial: State? = null

        /** An optional [Scheduler] used for dispatching state changes. */
        var observeOn: Scheduler? = null

        /** An optional [Scheduler] used for reduce function. */
        var reduceOn: Scheduler? = null
    }
}