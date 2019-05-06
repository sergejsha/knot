package de.halfbit.knot

import kotlin.reflect.KClass

interface Prime<State : Any, Change : Any, Action : Any> {
    fun addTo(composition: Composition<State, Change, Action>)
}

internal class DefaultPrime<State : Any, Change : Any, Action : Any>(
    private val reducers: Map<KClass<out Change>, Reduce<State, Change, Action>>,
    private val eventTransformers: List<EventTransformer<Change>>,
    private val actionTransformers: List<ActionTransformer<Action, Change>>
) : Prime<State, Change, Action> {

    override fun addTo(composition: Composition<State, Change, Action>) {
        composition.reducers.putAll(reducers)
        composition.eventTransformers.addAll(eventTransformers)
        composition.actionTransformers.addAll(actionTransformers)
    }
}