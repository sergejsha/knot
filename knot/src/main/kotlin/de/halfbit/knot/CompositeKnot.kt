package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * If your [Knot] becomes big and you want to improve its readability and maintainability, you may consider
 * to decompose it. You start decomposition by grouping related functionality into, in a certain sense,
 * indecomposable pieces called `Primes`.
 *
 * [Flowchart diagram](https://github.com/beworker/knot/raw/master/docs/diagrams/flowchart-composite-knot.png)
 *
 * Each `Prime` is isolated from the other `Primes`. It defines its own set of `Changes`, `Actions` and
 * `Reducers`. It's only the `State`, that is shared between the `Primes`. In that respect each `Prime` can
 * be seen as a separate [Knot] working on a shared `State`.
 *
 * Once all `Primes` are registered at a `CompositeKnot`, the knot can be finally composed using
 * [compose] function and start operating.
 */
interface CompositeKnot<State : Any> : Knot<State, Any> {

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
    private val actionInterceptors: MutableList<Interceptor<Any>>,
    private val actionSubject: Subject<Any>
) : CompositeKnot<State> {

    private val reducers = mutableMapOf<KClass<out Any>, Reducer<State, Any, Any>>()
    private val actionTransformers = mutableListOf<ActionTransformer<Any, Any>>()
    private val eventSources = mutableListOf<EventSource<Any>>()
    private val coldEventSources = lazy { mutableListOf<EventSource<Any>>() }
    private val composed = AtomicBoolean(false)
    private val disposables = CompositeDisposable()

    private val stateSubject = BehaviorSubject.create<State>()
    private val changeSubject = PublishSubject.create<Any>()

    private val subscriberCount = AtomicInteger()
    private var coldEventsDisposable: Disposable? = null
    private var coldEventsObservable: Observable<Any>? = null

    override fun <Change : Any, Action : Any> registerPrime(
        block: PrimeBuilder<State, Change, Action>.() -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        PrimeBuilder(
            reducers,
            eventSources,
            coldEventSources,
            actionTransformers,
            stateInterceptors,
            changeInterceptors,
            actionInterceptors
        ).also(block as PrimeBuilder<State, Any, Any>.() -> Unit)
    }

    override fun isDisposed(): Boolean = disposables.isDisposed
    override fun dispose() = disposables.dispose()

    override val state: Observable<State> = stateSubject
        .doOnSubscribe { if (subscriberCount.getAndIncrement() == 0) maybeSubscribeColdEvents() }
        .doFinally { if (subscriberCount.decrementAndGet() == 0) maybeUnsubscribeEvents() }

    override val change: Consumer<Any> = Consumer {
        check(composed.get()) { "compose() must be called before emitting any change." }
        changeSubject.onNext(it)
    }

    @Synchronized
    private fun maybeSubscribeColdEvents() {
        if (subscriberCount.get() > 0) {
            val coldEventsDisposable = this.coldEventsDisposable
            if (coldEventsDisposable == null) {
                var coldEventsObservable = this.coldEventsObservable
                if (coldEventsObservable == null && coldEventSources.isInitialized()) {
                    coldEventsObservable = Observable.merge(
                        mutableListOf<Observable<Any>>().apply {
                            coldEventSources.value.map { source -> this += source() }
                        }
                    )
                    this.coldEventsObservable = coldEventsObservable
                }
                if (coldEventsObservable != null) {
                    this.coldEventsDisposable = coldEventsObservable
                        .subscribe(
                            changeSubject::onNext,
                            changeSubject::onError
                        )
                }
            }
        }
    }

    @Synchronized
    private fun maybeUnsubscribeEvents() {
        coldEventsDisposable?.let {
            it.dispose()
            coldEventsDisposable = null
        }
    }

    override fun compose() {
        check(!composed.getAndSet(true)) { "compose() must be called just once." }
        disposables.add(
            Observable
                .merge(
                    mutableListOf<Observable<Any>>().apply {
                        this += changeSubject
                        actionSubject
                            .intercept(actionInterceptors)
                            .bind(actionTransformers) { this += it }
                        eventSources
                            .map { source -> this += source() }
                    }
                )
                .let { stream -> reduceOn?.let { stream.observeOn(it) } ?: stream }
                .serialize()
                .intercept(changeInterceptors)
                .scan(initialState) { state, change ->
                    val reducer = reducers[change::class] ?: error("Cannot find reducer for $change")
                    reducer(state, change).emitActions(actionSubject)
                }
                .distinctUntilChanged { prev, curr -> prev === curr }
                .let { stream -> observeOn?.let { stream.observeOn(it) } ?: stream }
                .intercept(stateInterceptors)
                .subscribe(
                    stateSubject::onNext,
                    stateSubject::onError
                )
        )
        maybeSubscribeColdEvents()
    }
}
