package de.halfbit.interstate.dsl

import de.halfbit.interstate.CommandReducer
import de.halfbit.interstate.EventReducer
import de.halfbit.interstate.Statechart
import io.reactivex.Observable

@DslMarker
internal annotation class StatechartDsl

@StatechartDsl
class StatechartBuilder<S : Any, C : Any>
internal constructor() {

    inline fun <reified S1 : S> state(block: State<S1, C>.() -> Unit) {
        TODO()
    }

    internal fun build(): Statechart<S, C> {
        TODO()
    }

    fun State<out S, C>.transition(state: S): Observable<in S> {
        TODO()
    }

}

class State<S : Any, C : Any> {

    inline fun <reified C1 : C> on(block: CommandReducer<C1, S>.() -> Observable<in S>) {
    }

    inline fun <reified E : Any> on(event: Observable<E>, block: EventReducer<E, S>.() -> Observable<C>) {
    }

}

fun <S : Any, C : Any> statechart(block: StatechartBuilder<S, C>.() -> Unit) =
    StatechartBuilder<S, C>().apply(block).build()