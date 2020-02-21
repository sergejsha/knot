package de.halfbit.knot

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
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
    initialState: State,
    observeOn: Scheduler?,
    reduceOn: Scheduler?,
    stateInterceptors: MutableList<Interceptor<State>>,
    changeInterceptors: MutableList<Interceptor<Any>>,
    actionInterceptors: MutableList<Interceptor<Any>>,
    actionSubject: Subject<Any>
) : CompositeKnot<State> {

    private val coldEventSources = lazy { mutableListOf<EventSource<Any>>() }
    private val composition = AtomicReference(
        Composition(
            initialState,
            observeOn,
            reduceOn,
            stateInterceptors,
            changeInterceptors,
            actionInterceptors,
            actionSubject
        )
    )

    private val stateSubject = BehaviorSubject.create<State>()
    private val changeSubject = PublishSubject.create<Any>()
    private val disposables = CompositeDisposable()

    private val subscriberCount = AtomicInteger()
    private var coldEventsDisposable: Disposable? = null
    private var coldEventsObservable: Observable<Any>? = null

    @Suppress("UNCHECKED_CAST")
    override fun <Change : Any, Action : Any> registerPrime(
        block: PrimeBuilder<State, Change, Action>.() -> Unit
    ) {
        composition.get()?.let {
            PrimeBuilder(
                it.reducers,
                it.eventSources,
                coldEventSources,
                it.actionTransformers,
                it.stateInterceptors,
                it.changeInterceptors,
                it.actionInterceptors
            ).also(block as PrimeBuilder<State, Any, Any>.() -> Unit)
        } ?: error("Primes cannot be registered after compose() was called")
    }

    override fun isDisposed(): Boolean = disposables.isDisposed
    override fun dispose() = disposables.dispose()

    override val state: Observable<State> = stateSubject
        .doOnSubscribe { if (subscriberCount.getAndIncrement() == 0) maybeSubscribeColdEvents() }
        .doFinally { if (subscriberCount.decrementAndGet() == 0) maybeUnsubscribeColdEvents() }

    override val change: Consumer<Any> = Consumer {
        check(composition.get() == null) { "compose() must be called before emitting any change." }
        changeSubject.onNext(it)
    }

    @Synchronized
    private fun maybeSubscribeColdEvents() {
        if (coldEventSources.isInitialized() &&
            coldEventsDisposable == null &&
            subscriberCount.get() > 0
        ) {
            val coldEventsObservable =
                this.coldEventsObservable
                    ?: coldEventSources
                        .mergeIntoObservable()
                        .also { this.coldEventsObservable = it }

            coldEventsDisposable =
                coldEventsObservable
                    .subscribe(
                        changeSubject::onNext,
                        changeSubject::onError
                    )
        }
    }

    @Synchronized
    private fun maybeUnsubscribeColdEvents() {
        coldEventsDisposable?.let {
            it.dispose()
            coldEventsDisposable = null
        }
    }

    override fun compose() {
        composition.getAndSet(null)?.let { composition ->
            disposables.add(
                Observable
                    .merge(
                        mutableListOf<Observable<Any>>().apply {
                            this += changeSubject

                            composition.actionSubject
                                .intercept(composition.actionInterceptors)
                                .bind(composition.actionTransformers) { this += it }

                            composition.eventSources
                                .map { source -> this += source() }
                        }
                    )
                    .let { stream -> composition.reduceOn?.let { stream.observeOn(it) } ?: stream }
                    .serialize()
                    .intercept(composition.changeInterceptors)
                    .scan(composition.initialState) { state, change ->
                        val reducer = composition.reducers[change::class]
                            ?: error("Cannot find reducer for $change")
                        reducer(state, change).emitActions(composition.actionSubject)
                    }
                    .distinctUntilChanged { prev, curr -> prev === curr }
                    .let { stream -> composition.observeOn?.let { stream.observeOn(it) } ?: stream }
                    .intercept(composition.stateInterceptors)
                    .subscribe(
                        stateSubject::onNext,
                        stateSubject::onError
                    )
            )
            maybeSubscribeColdEvents()
        } ?: error("compose() must be called just once.")
    }

    private class Composition<State : Any>(
        val initialState: State,
        val observeOn: Scheduler?,
        val reduceOn: Scheduler?,
        val stateInterceptors: MutableList<Interceptor<State>>,
        val changeInterceptors: MutableList<Interceptor<Any>>,
        val actionInterceptors: MutableList<Interceptor<Any>>,
        val actionSubject: Subject<Any>
    ) {
        val reducers = mutableMapOf<KClass<out Any>, Reducer<State, Any, Any>>()
        val actionTransformers = mutableListOf<ActionTransformer<Any, Any>>()
        val eventSources = mutableListOf<EventSource<Any>>()
    }
}
