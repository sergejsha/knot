package de.halfbit.knot

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

interface Knot<State : Any, Change : Any, Action : Any> {
    val state: Observable<State>
    val change: Consumer<Change>
    val disposable: Disposable
}

class Effect<State : Any, Action : Any>(
    val state: State,
    val action: Action? = null
)
