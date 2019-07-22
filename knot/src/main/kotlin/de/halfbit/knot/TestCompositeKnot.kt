package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Creates a [TestCompositeKnot]. This function should only be used in tests.
 * For creating a productive composite knot use [compositeKnot] function.
 */
fun <State : Any> testCompositeKnot(
    block: CompositeKnotBuilder<State>.() -> Unit
): TestCompositeKnot<State> {
    val actionSubject = PublishSubject.create<Any>()
    return CompositeKnotBuilder<State>()
        .also(block)
        .build(actionSubject)
        .let { compositeKnot ->
            DefaultTestCompositeKnot(
                compositeKnot,
                actionSubject
            )
        }
}

/**
 * `TestCompositeKnot` is used for testing knot primes in isolation. Create test composition
 * knot, add a knot `Prime` to it you want to test and start testing it. In addition to standard
 * `CompositeKnot` functionality `TestCompositeKnot` lets you observe and emit actions.
 */
interface TestCompositeKnot<State : Any> : CompositeKnot<State> {

    /** Actions observer to be used in tests. */
    val action: Observable<Any>

    /** Actions consumer to be used in tests. */
    val actionConsumer: Consumer<Any>
}

internal class DefaultTestCompositeKnot<State : Any>(
    private val compositeKnot: DefaultCompositeKnot<State>,
    private val actionSubject: PublishSubject<Any>
) : TestCompositeKnot<State> {

    private val composed = AtomicBoolean()

    override val action: Observable<Any> = actionSubject
    override val actionConsumer: Consumer<Any> = Consumer {
        check(composed.get()) { "compose() must be called before emitting actions" }
        actionSubject.onNext(it)
    }

    override fun <Change : Any, Action : Any> registerPrime(
        block: PrimeBuilder<State, Change, Action>.() -> Unit
    ) = compositeKnot.registerPrime(block)

    override fun compose() {
        compositeKnot.compose()
        composed.set(true)
    }

    override val change: Consumer<Any> = compositeKnot.change
    override val state: Observable<State> = compositeKnot.state
    override fun isDisposed(): Boolean = compositeKnot.isDisposed
    override fun dispose() = compositeKnot.dispose()
}
