package de.halfbit.knot

import kotlin.reflect.KClass

fun <State : Any, Change : Any, Action : Any> prime(
    block: PrimeBuilder<State, Change, Action>.() -> Unit
): Prime<State, Change, Action> =
    PrimeBuilder<State, Change, Action>()
        .also(block)
        .build()

@DslMarker
annotation class PrimeDsl

@PrimeDsl
class PrimeBuilder<State : Any, Change : Any, Action : Any>
internal constructor() {
    private val reducers = mutableMapOf<KClass<out Change>, Reduce<State, Change, Action>>()
    private val eventTransformers = mutableListOf<EventTransformer<Change>>()
    private val actionTransformers = mutableListOf<ActionTransformer<Action, Change>>()

    fun state(block: StateBuilder<State, Change, Action>.() -> Unit) {
        StateBuilder(reducers).also(block)
    }

    fun actions(block: ActionBuilder<Change, Action>.() -> Unit) {
        ActionBuilder(actionTransformers).also(block)
    }

    fun events(block: EventBuilder<Change>.() -> Unit) {
        EventBuilder(eventTransformers).also(block)
    }

    fun build(): Prime<State, Change, Action> = DefaultPrime(
        reducers = reducers,
        eventTransformers = eventTransformers,
        actionTransformers = actionTransformers
    )

    @PrimeDsl
    class StateBuilder<State : Any, Change : Any, Action : Any>
    internal constructor(
        private val reducers: MutableMap<KClass<out Change>, Reduce<State, Change, Action>>
    ) {
        fun reduce(changeType: KClass<out Change>, reduce: Reduce<State, Change, Action>) {
            reducers[changeType] = reduce
        }

        inline fun <reified C : Change> reduce(noinline reduce: Reduce<State, C, Action>) {
            @Suppress("UNCHECKED_CAST")
            this.reduce(C::class, reduce as Reduce<State, Change, Action>)
        }

        fun State.only(): Effect<State, Action> = Effect(this)
        infix fun State.and(action: Action) = Effect(this, action)
    }

    @PrimeDsl
    class ActionBuilder<Change : Any, Action : Any>
    internal constructor(
        private val actionTransformers: MutableList<ActionTransformer<Action, Change>>
    ) {
        fun performAny(transformer: ActionTransformer<Action, Change>) {
            actionTransformers += transformer
        }

        inline fun <reified A : Action> perform(noinline transformer: ActionTransformer<A, Change>) {
            performAny(TypedActionTransformer(A::class.java, transformer))
        }
    }

    @PrimeDsl
    class EventBuilder<Change : Any>
    internal constructor(
        private val eventTransformers: MutableList<EventTransformer<Change>>
    ) {
        fun transform(transformer: EventTransformer<Change>) {
            eventTransformers += transformer
        }
    }
}
