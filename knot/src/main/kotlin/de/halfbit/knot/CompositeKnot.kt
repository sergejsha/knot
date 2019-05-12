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
 * in a certain sense, indecomposable pieces called `Primes`.
 *
 * [Flowchart diagram](https://github.com/beworker/knot/raw/master/docs/diagrams/flowchart-composite-knot.png)
 *
 * [Prime] defines its own [Change]'s, [Action]'s and [Reducer] for own changes. It's only the [State], what
 * is shared between the `Primes`. In that respect each `Prime` can be considered to be a separate [Knot]
 * working on a shared `State`. Once all `Primes` are defined, they can be composed together and provided
 * though [compose] function to [CompositeKnot] which implements standard [Knot] interface.
 */
interface CompositeKnot<State : Any, Change : Any, Action : Any> : Knot<State, Change, Action> {
    fun compose(composition: Composition<State, Change, Action>)
}

typealias StateTrigger<State, Change> = (state: Observable<State>) -> Observable<Change>

class Composition<State : Any, Change : Any, Action : Any> {
    val reducers = mutableMapOf<KClass<out Change>, Reducer<State, Change, Action>>()
    val actionTransformers = mutableListOf<ActionTransformer<Action, Change>>()
    val eventTransformers = mutableListOf<EventTransformer<Change>>()
    val stateInterceptors = mutableListOf<Interceptor<State>>()
    val changeInterceptors = mutableListOf<Interceptor<Change>>()
    val actionInterceptors = mutableListOf<Interceptor<Action>>()
    val stateTriggers = mutableListOf<StateTrigger<State, Change>>()
}

internal class DefaultCompositeKnot<State : Any, Change : Any, Action : Any>(
    private val initialState: State,
    private val observeOn: Scheduler?,
    private val reduceOn: Scheduler?,
    private val stateInterceptors: List<Interceptor<State>>,
    private val changeInterceptors: List<Interceptor<Change>>,
    private val actionInterceptors: List<Interceptor<Action>>
) : CompositeKnot<State, Change, Action> {

    private val stateSubject = BehaviorSubject.create<State>()
    private val changeSubject = PublishSubject.create<Change>()
    private val actionSubject = PublishSubject.create<Action>()
    private val composed = AtomicBoolean(false)

    override val state = stateSubject
    override val disposable = CompositeDisposable()
    override val change = Consumer<Change> {
        if (!composed.get()) {
            error("compose() must be called before emitting any change.")
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
                    this += changeSubject
                    actionSubject
                        .intercept(actionInterceptors)
                        .intercept(composition.actionInterceptors)
                        .let { action ->
                            composition.actionTransformers.map { transform ->
                                this += transform(action)
                            }
                        }
                    composition.eventTransformers.map { transformer -> this += transformer() }
                    composition.stateTriggers.map { trigger -> this += trigger(stateSubject) }
                }
            )
            .let { change -> reduceOn?.let { change.observeOn(it) } ?: change }
            .intercept(changeInterceptors)
            .intercept(composition.changeInterceptors)
            .serialize()
            .scan(initialState) { state, change ->
                val reducer = composition.reducers[change::class] ?: error("Cannot find reducer for $change")
                reducer(state, change)
                    .also { it.action?.let { action -> actionSubject.onNext(action) } }
                    .state
            }
            .distinctUntilChanged()
            .let { state -> observeOn?.let { state.observeOn(it) } ?: state }
            .intercept(stateInterceptors)
            .intercept(composition.stateInterceptors)
            .doOnSubscribe { composed.set(true) }
            .subscribe(stateSubject)
    }
}
