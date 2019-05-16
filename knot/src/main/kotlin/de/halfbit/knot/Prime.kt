package de.halfbit.knot

import kotlin.reflect.KClass

interface Prime<State : Any, Change : Any, Action : Any> {
    fun addTo(composition: Composition<State, Change, Action>)
}

internal class DefaultPrime<State : Any, Change : Any, Action : Any>(
    private val reducers: Map<KClass<out Change>, Reducer<State, Change, Action>>,
    private val eventSources: List<EventSource<Change>>,
    private val actionTransformers: List<ActionTransformer<Action, Change>>,
    private val stateInterceptors: List<Interceptor<State>>,
    private val changeInterceptors: List<Interceptor<Change>>,
    private val actionInterceptors: List<Interceptor<Action>>,
    private val stateTriggers: List<StateTrigger<State, Change>>
) : Prime<State, Change, Action> {

    override fun addTo(composition: Composition<State, Change, Action>) {
        composition.reducers.putAll(reducers)
        composition.eventSources.addAll(eventSources)
        composition.actionTransformers.addAll(actionTransformers)
        composition.stateInterceptors.addAll(stateInterceptors)
        composition.changeInterceptors.addAll(changeInterceptors)
        composition.actionInterceptors.addAll(actionInterceptors)
        composition.stateTriggers.addAll(stateTriggers)
    }
}