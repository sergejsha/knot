package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

interface CompositeKnot<State : Any, Change : Any, Action : Any> : Knot<State, Change, Action> {
    fun compose(composition: Composition<State, Change, Action>)
}

class Composition<State : Any, Change : Any, Action : Any> {
    val reducers = mutableMapOf<KClass<out Change>, Reduce<State, Change, Action>>()
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
                reducer(change, state)
                    .also { it.action?.let { action -> actionSubject.onNext(action) } }
                    .state
            }
            .let { state -> observeOn?.let { state.observeOn(it) } ?: state }
            .distinctUntilChanged()
            .doOnSubscribe { composed.set(true) }
            .subscribe(stateSubject)
    }

}