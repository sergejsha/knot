package de.halfbit.knot.dsl

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface WithState<State : Any> {
    val state: State
}

interface WithStateReduce<State : Any> : WithState<State> {
    fun reduceState(reducer: Reducer<State>): Reducer<State>

    fun <Type : Any> Observable<Type>.mapReduceState(reducer: MapStateReducer<State, Type>)
            : Observable<Reducer<State>> = this.map<Reducer<State>> { { reducer(it) } }

    fun <Type : Any> Observable<Type>.switchMapReduceState(reducer: FlatMapStateReducer<State, Type>)
            : Observable<Reducer<State>> = this.switchMap<Reducer<State>> { reducer(it) }

    fun <Type : Any> Observable<Type>.flatMapReduceState(reducer: FlatMapStateReducer<State, Type>)
            : Observable<Reducer<State>> = this.flatMap<Reducer<State>> { reducer(it) }

    fun <Type : Any> Observable<Type>.switchMapSingleReduceState(reducer: FlatMapSingleStateReducer<State, Type>)
            : Observable<Reducer<State>> = this.switchMapSingle<Reducer<State>> { reducer(it) }

    fun Observable<Reducer<State>>.onErrorReduceState(reducer: ErrorStateReducer<State>)
            : Observable<Reducer<State>> = this.onErrorReturn { { reducer(it) } }

    fun Completable.andThenReduceState(reducer: Reducer<State>)
            : Observable<Reducer<State>> = Observable.just(reducer)

    fun <Type : Any> Single<Type>.mapReduceState(reducer: MapStateReducer<State, Type>)
            : Single<Reducer<State>> = this.map<Reducer<State>> { { reducer(it) } }
}

typealias MapStateReducer<State, Type> = WithState<State>.(type: Type) -> State
typealias FlatMapStateReducer<State, Type> = WithState<State>.(input: Type) -> Observable<Reducer<State>>
typealias FlatMapSingleStateReducer<State, Type> = WithState<State>.(input: Type) -> Single<Reducer<State>>
typealias ErrorStateReducer<State> = WithState<State>.(error: Throwable) -> State
typealias Reducer<State> = WithState<State>.() -> State
