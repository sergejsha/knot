package de.halfbit.knot

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import kotlin.reflect.KClass

interface Composition<State : Any, Change : Any, Action : Any> {

    interface Delegate
    interface Reducer<State : Any, Change : Any, Action : Any> {
        fun reduce(state: State, change: Change): Effect<State, Action>
    }

    interface Resolver<Action : Any, Change : Any> {
        fun resolve(action: Action): Maybe<Change>
    }

    val state: Observable<State>
    val change: Consumer<Change>
    val disposable: CompositeDisposable

    fun register(delegate: Delegate, type: KClass<Delegate>)
    fun register(reducer: Reducer<State, Change, Action>, type: KClass<Change>)
    fun register(resolver: Resolver<Action, Change>, type: KClass<Action>)

    fun knot(): Knot<State, Change, Action>
}