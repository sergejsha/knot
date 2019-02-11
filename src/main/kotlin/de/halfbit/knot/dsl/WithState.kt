package de.halfbit.knot.dsl

import io.reactivex.Completable
import io.reactivex.Observable

interface WithState<State : Any> {
    val state: State
}

interface WithStateReduce<State : Any> : WithState<State> {
    fun reduce(reducer: Reducer<State>): Reducer<State>

    fun <Input : Any> Observable<Input>.mapReduceState(reducer: MapStateReducer<State, Input>):
            Observable<Reducer<State>> = this.map<Reducer<State>> { { reducer(it) } }

    fun <Input : Any> Observable<Input>.switchMapReduceState(reducer: SwitchMapStateReducer<State, Input>):
            Observable<Reducer<State>> = this.switchMap<Reducer<State>> { reducer(it) }

    fun Observable<Reducer<State>>.onErrorReduceState(reducer: ErrorStateReducer<State>):
            Observable<Reducer<State>> = this.onErrorReturn { { reducer(it) } }

    fun Completable.andThenReduceState(reducer: Reducer<State>): Observable<Reducer<State>> =
        Observable.just(reducer)
}

typealias MapStateReducer<State, Input> = WithState<State>.(input: Input) -> State
typealias SwitchMapStateReducer<State, Input> = WithState<State>.(input: Input) -> Observable<Reducer<State>>
typealias ErrorStateReducer<State> = WithState<State>.(error: Throwable) -> State
typealias Reducer<State> = WithState<State>.() -> State
