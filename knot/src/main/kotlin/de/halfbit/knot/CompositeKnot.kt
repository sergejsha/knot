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
interface CompositeKnot<State : Any> : Store<State> {
    fun <Change : Any, Action : Any> getComposition(): Composition<State, Change, Action>
    fun compose()
}

interface Composition<State : Any, Change : Any, Action : Any> {
    fun registerPrime(block: PrimeBuilder<State, Change, Action>.() -> Unit)
}

/**
 * Interface for testing [CompositeKnot] and its `Primes` by allowing emitting `Changes`.
 */
interface TestCompositeKnot<State : Any, Change : Any> :
    CompositeKnot<State>, Knot<State, Change>

internal class DefaultCompositeKnot<State : Any>(
    private val initialState: State,
    private val observeOn: Scheduler?,
    private val reduceOn: Scheduler?,
    private val stateInterceptors: MutableList<Interceptor<State>>,
    private val changeInterceptors: MutableList<Interceptor<Any>>,
    private val actionInterceptors: MutableList<Interceptor<Any>>
) : CompositeKnot<State>, TestCompositeKnot<State, Any> {

    private val stateSubject = BehaviorSubject.create<State>()
    private val changeSubject = PublishSubject.create<Any>()
    private val actionSubject = PublishSubject.create<Any>()
    private val composed = AtomicBoolean(false)
    private val composition = object : Composition<State, Any, Any> {

        val reducers = mutableMapOf<KClass<out Any>, Reducer<State, Any, Any>>()
        val actionTransformers = mutableListOf<ActionTransformer<Any, Any>>()
        val eventSources = mutableListOf<EventSource<Any>>()

        override fun registerPrime(
            block: PrimeBuilder<State, Any, Any>.() -> Unit
        ) {
            PrimeBuilder(
                reducers,
                eventSources,
                actionTransformers,
                stateInterceptors,
                changeInterceptors,
                actionInterceptors
            ).also(block)
        }
    }


    override val state = stateSubject
    override val change: Consumer<Any> = Consumer { changeSubject.onNext(it) }
    override val disposable = CompositeDisposable()

    override fun <Change : Any, Action : Any> getComposition(): Composition<State, Change, Action> {
        @Suppress("UNCHECKED_CAST")
        return composition as Composition<State, Change, Action>
    }

    override fun compose() {
        if (composed.getAndSet(true)) {
            error("compose() must be called just once.")
        }
        Observable
            .merge(
                mutableListOf<Observable<Any>>().apply {
                    this += changeSubject
                    actionSubject
                        .intercept(actionInterceptors)
                        .bind(composition.actionTransformers) { this += it }
                    composition.eventSources.map { source -> this += source() }
                }
            )
            .let { change -> reduceOn?.let { change.observeOn(it) } ?: change }
            .intercept(changeInterceptors)
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
            .subscribe(stateSubject)
    }
}
