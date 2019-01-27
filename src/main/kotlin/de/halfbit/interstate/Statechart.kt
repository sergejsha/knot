package de.halfbit.interstate

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.functions.Consumer

interface Statechart<S : Any, C : Any> {

    sealed class StateEvent {
        object Entry : StateEvent()
        object Exit : StateEvent()
    }

    val state: Observable<S>
    val command: Consumer<C>
}

internal class DefaultStatechart<S : Any, C : Any> : Statechart<S, C> {

    override val state = BehaviorRelay.create<S>().toSerialized()
    override val command = PublishRelay.create<C>().toSerialized()

}

interface CommandReducer<C : Any, S : Any> : Reducer<S> {
    val command: Observable<C>
}

interface EventReducer<E : Any, S : Any> : Reducer<S> {
    val event: Observable<E>
}

interface StateEventReducer<E : Statechart.StateEvent, S : Any> : Reducer<S> {
    val event: Observable<E>
}

interface Reducer< S : Any> {
    val currentState: S
}