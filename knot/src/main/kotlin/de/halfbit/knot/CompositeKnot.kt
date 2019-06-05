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
 * If your [Knot] becomes big and you want to improve its redability and maintainability, you may consider
 * to decompose it. You start decomposition by grouping related functionality into, in a certain sense,
 * indecomposable pieces called `Primes`.
 *
 * [Flowchart diagram](https://github.com/beworker/knot/raw/master/docs/diagrams/flowchart-composite-knot.png)
 *
 * Each `Prime` is isolated from the other `Primes`. It defines its own set of `Changes`, `Actions` and
 * `Reducers`. It's only the `State`, what is shared between the `Primes`. In that respect each `Prime` can
 * be seen as a separate [Knot] working on a shared `State`.
 *
 * Once all `Primes` are registered at a `CompositeKnot`, the knot can be finally composed using
 * [compose] function and start operating.
 */
interface CompositeKnot<State : Any> : Knot<State, Any> {

    /** Change emitter used for delivering changes to this knot. */
    override val change: Consumer<Any>

    /** Registers a new `Prime` at this composite knot. */
    fun <Change : Any, Action : Any> registerPrime(block: PrimeBuilder<State, Change, Action>.() -> Unit)

    /** Finishes composition of `Primes` and moves this knot into operational mode. */
    fun compose()
}

internal class DefaultCompositeKnot<State : Any>(
    private val initialState: State,
    private val observeOn: Scheduler?,
    private val reduceOn: Scheduler?,
    private val stateInterceptors: MutableList<Interceptor<State>>,
    private val changeInterceptors: MutableList<Interceptor<Any>>,
    private val actionInterceptors: MutableList<Interceptor<Any>>
) : CompositeKnot<State> {

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
        check(composed.get()) { "compose() must be called before emitting any change." }
        changeSubject.onNext(it)
    }

    override fun compose() {
        check(!composed.getAndSet(true)) { "compose() must be called just once." }
        disposable.add(
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
                .let { stream -> reduceOn?.let { stream.observeOn(it) } ?: stream }
                .intercept(changeInterceptors)
                .serialize()
                .scan(initialState) { state, change ->
                    val reducer = reducers[change::class] ?: error("Cannot find reducer for $change")
                    reducer(state, change)
                        .also { it.action?.let { action -> actionSubject.onNext(action) } }
                        .state
                }
                .intercept(stateInterceptors)
                .distinctUntilChanged()
                .let { stream -> observeOn?.let { stream.observeOn(it) } ?: stream }
                .subscribe(
                    { stateSubject.onNext(it) },
                    { stateSubject.onError(it) }
                )
        )
    }
}
