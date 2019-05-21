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
    fun <Change : Any, Action : Any> registerPrime(block: PrimeBuilder<State, Change, Action>.() -> Unit)
    fun compose()
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

    private val reducers = mutableMapOf<KClass<out Any>, Reducer<State, Any, Any>>()
    private val actionTransformers = mutableListOf<ActionTransformer<Any, Any>>()
    private val eventSources = mutableListOf<EventSource<Any>>()
    private val composed = AtomicBoolean(false)

    private val stateSubject = BehaviorSubject.create<State>()
    private val changeSubject = PublishSubject.create<Any>()
    private val actionSubject = PublishSubject.create<Any>()

    override fun <Change : Any, Action : Any> registerPrime(
        block: PrimeBuilder<State, Change, Action>.() -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        PrimeBuilder(
            reducers,
            eventSources,
            actionTransformers,
            stateInterceptors,
            changeInterceptors,
            actionInterceptors
        ).also(block as PrimeBuilder<State, Any, Any>.() -> Unit)
    }

    override val state = stateSubject
    override val disposable = CompositeDisposable()
    override val change: Consumer<Any> = Consumer {
        if (!composed.get()) {
            error("compose() must be called before emitting any change.")
        }
        changeSubject.onNext(it)
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
                        .bind(actionTransformers) { this += it }
                    eventSources.map { source -> this += source() }
                }
            )
            .let { change -> reduceOn?.let { change.observeOn(it) } ?: change }
            .intercept(changeInterceptors)
            .serialize()
            .scan(initialState) { state, change ->
                val reducer = reducers[change::class] ?: error("Cannot find reducer for $change")
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
