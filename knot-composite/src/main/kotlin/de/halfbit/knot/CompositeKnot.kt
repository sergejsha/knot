package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * If your [Knot] becomes big and you want to improve its maintainability and extensibility you
 * may consider to decompose it. You start decomposition by grouping related functionality into,
 * in a certain sense, indecomposable pieces called [Prime]'s.
 *
 * [Flowchart diagram](https://github.com/beworker/knot/raw/master/docs/diagrams/flowchart-composite-knot.png)
 *
 * [Prime] defines its own [Change]'s, [Action]'s and *Reducer* for own changes. It's only the [State], what
 * is shared between the [Prime]'s. In that respect each [Prime] can be considered to be a separate knot
 * working on a shared [State]. Once all [Prime]'s are defined, they can be composed together and provided
 * though [compose] function to [CompositeKnot] which implements standard knot interface.
 */
interface CompositeKnot<State : Any, Change : Any, Action : Any> : Knot<State, Change, Action> {
    fun compose(composition: Composition<State, Change, Action>)
}

class Composition<State : Any, Change : Any, Action : Any> {
    val reducers = mutableMapOf<KClass<out Change>, Reducer<State, Change, Action>>()
    val actionTransformers = mutableListOf<ActionTransformer<Action, Change>>()
    val eventTransformers = mutableListOf<EventTransformer<Change>>()
}

internal class DefaultCompositeKnot<State : Any, Change : Any, Action : Any>(
    private val initialState: State,
    private val observeOn: Scheduler?,
    private val reduceOn: Scheduler?
) : CompositeKnot<State, Change, Action> {

    private val stateSubject = BehaviorSubject.create<State>()
    private val changeSubject = PublishSubject.create<Change>()
    private val actionSubject = PublishSubject.create<Action>()
    private val composed = AtomicBoolean(false)

    override val state = stateSubject
    override val disposable = CompositeDisposable()
    override val change = Consumer<Change> {
        if (!composed.get()) {
            error("Call compose() before dispatching any change.")
        }
        changeSubject.onNext(it)
    }

    override fun compose(composition: Composition<State, Change, Action>) {
        if (composed.get()) {
            error("compose() must be called just once.")
        }
        Observable
            .merge(
                mutableListOf<Observable<Change>>().apply {
                    add(changeSubject)
                    composition.eventTransformers.map { add(it.invoke()) }
                    composition.actionTransformers.map { add(it.invoke(actionSubject)) }
                }
            )
            .let { change -> reduceOn?.let { change.observeOn(it) } ?: change }
            .serialize()
            .scan(initialState) { state, change ->
                val reducer = composition.reducers[change::class] ?: error("Cannot find reducer for $change")
                reducer(state, change)
                    .also { it.action?.let { action -> actionSubject.onNext(action) } }
                    .state
            }
            .let { state -> observeOn?.let { state.observeOn(it) } ?: state }
            .distinctUntilChanged()
            .doOnSubscribe { composed.set(true) }
            .subscribe(stateSubject)
    }

}