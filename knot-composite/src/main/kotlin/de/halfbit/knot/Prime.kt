package de.halfbit.knot

import kotlin.reflect.KClass

interface Prime<State : Any, Change : Any, Action : Any> {
    fun compose(composite: Composition<State, Change, Action>)
}

internal class DefaultPrime<State : Any, Change : Any, Action : Any>(
    private val reducers: Map<KClass<out Change>, Reduce<State, Change, Action>>,
    private val eventTransformers: List<EventTransformer<Change>>,
    private val actionTransformers: List<ActionTransformer<Action, Change>>
) : Prime<State, Change, Action> {

    override fun compose(composite: Composition<State, Change, Action>) {
        composite.reducers.putAll(reducers)
        composite.eventTransformers.addAll(eventTransformers)
        composite.actionTransformers.addAll(actionTransformers)
    }
}