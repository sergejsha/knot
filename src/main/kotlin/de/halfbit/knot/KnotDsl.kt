package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import kotlin.reflect.KClass

fun <State : Any, Command : Any, Change : Any> knot(
    block: KnotBuilder<State, Command, Change>.() -> Unit
): Knot<State, Command, Change> =
    KnotBuilder<State, Command, Change>()
        .also(block)
        .build()

@DslMarker
annotation class KnotDsl

@KnotDsl
class KnotBuilder<State : Any, Command : Any, Change : Any>
internal constructor() {
    private var initialState: State? = null
    private var reducer: Reducer<State, Change>? = null
    private var observeOn: Scheduler? = null
    private var reduceOn: Scheduler? = null
    private val eventTransformers = mutableListOf<EventTransformer<Change>>()
    private val commandTransformers = mutableListOf<CommandTransformer<Command, Change>>()

    fun state(block: OnState<State, Change>.() -> Unit) {
        OnState<State, Change>()
            .also {
                block(it)
                initialState = it.initial
                reducer = it.reducer
                observeOn = it.observeOn
                reduceOn = it.reduceOn
            }
    }

    fun onEvent(transformer: EventTransformer<Change>) {
        eventTransformers += transformer
    }

    fun onCommand(transformer: CommandTransformer<Command, Change>) {
        commandTransformers += transformer
    }

    inline fun <reified T : Command> on(noinline transformer: CommandTransformer<T, Change>) {
        onCommand(TypedCommandTransformer(T::class, transformer))
    }

    fun build(): Knot<State, Command, Change> = DefaultKnot(
        initialState = checkNotNull(initialState) { "knot { state { initialState } } must be set" },
        reducer = checkNotNull(reducer) { "knot { state { reducer } } must be set" },
        observeOn = observeOn,
        reduceOn = reduceOn,
        eventTransformers = eventTransformers,
        commandTransformers = commandTransformers
    )
}

@KnotDsl
class OnState<State : Any, Change : Any>
internal constructor() {
    internal var reducer: Reducer<State, Change>? = null

    var initial: State? = null
    var observeOn: Scheduler? = null
    var reduceOn: Scheduler? = null

    fun reduce(reducer: Reducer<State, Change>) {
        this.reducer = reducer
    }
}

class Effect<State : Any, Change : Any>(
    val state: State,
    val action: Single<Change>?
)

typealias Reducer<State, Change> = WithEffect<State, Change>.(change: Change, state: State) -> Effect<State, Change>
typealias CommandTransformer<Command, Change> = (command: Observable<Command>) -> Observable<Change>
typealias EventTransformer<Change> = () -> Observable<Change>

class TypedCommandTransformer<Command : Any, Change : Any, C : Command>(
    private val type: KClass<C>,
    private val transform: CommandTransformer<C, Change>
) : CommandTransformer<Command, Change> {
    override fun invoke(command: Observable<Command>): Observable<Change> =
        transform(command.ofType(type.javaObjectType))
}
