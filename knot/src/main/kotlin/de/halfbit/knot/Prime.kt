package de.halfbit.knot

import kotlin.reflect.KClass

interface Prime<State : Any, Change : Any, Action : Any> {
    fun addTo(composition: Composition<State, Change, Action>)
}

internal class DefaultPrime<State : Any, Change : Any, Action : Any>(
    private val reducers: Map<KClass<out Change>, Reducer<State, Change, Action>>,
    private val eventTransformers: List<EventTransformer<Change>>,
    private val actionTransformers: List<ActionTransformer<Action, Change>>,
    private val stateInterceptors: List<Interceptor<State>>,
    private val changeInterceptors: List<Interceptor<Change>>,
    private val actionInterceptors: List<Interceptor<Action>>
) : Prime<State, Change, Action> {

    override fun addTo(composition: Composition<State, Change, Action>) {
        composition.reducers.putAll(reducers)
        composition.eventTransformers.addAll(eventTransformers)
        composition.actionTransformers.addAll(actionTransformers)
        composition.stateInterceptors.addAll(stateInterceptors)
        composition.changeInterceptors.addAll(changeInterceptors)
        composition.actionInterceptors.addAll(actionInterceptors)
    }
}