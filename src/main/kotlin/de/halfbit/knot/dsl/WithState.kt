package de.halfbit.knot.dsl

import io.reactivex.Observable

interface WithState<State : Any> {
    val state: State
}

interface WithStateReduce<State : Any> : WithState<State> {
    fun reduce(reducer: Reducer<State>): Reducer<State>

    fun <Input : Any> Observable<Input>.reduceState(reducer: MapStateReducer<State, Input>):
            Observable<Reducer<State>> = this.map<Reducer<State>> { { reducer(it) } }

    fun Observable<Reducer<State>>.reduceStateOnError(reducer: ErrorStateReducer<State>):
            Observable<Reducer<State>> = this.onErrorReturn { { reducer(it) } }
}

typealias MapStateReducer<State, Input> = WithState<State>.(input: Input) -> State
typealias ErrorStateReducer<State> = WithState<State>.(error: Throwable) -> State
typealias Reducer<State> = WithState<State>.() -> State
