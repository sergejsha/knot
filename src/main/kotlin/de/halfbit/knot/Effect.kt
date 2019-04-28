package de.halfbit.knot

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

class Effect<State : Any, Change : Any>(
    val state: State,
    val action: Action<Change>?
)

fun <State : Any, Change : Any> effect(state: State)
        : Effect<State, Change> = Effect(state, null)

fun <State : Any, Change : Any> effect(state: State, action: Single<Change>)
        : Effect<State, Change> = Effect(state, Action.Single(action))

fun <State : Any, Change : Any> effect(state: State, action: Maybe<Change>)
        : Effect<State, Change> = Effect(state, Action.Maybe(action))

fun <State : Any, Change : Any> effect(state: State, action: Completable)
        : Effect<State, Change> = Effect(state, Action.Completable(action))

fun <State : Any, Change : Any> effect(state: State, action: () -> Unit)
        : Effect<State, Change> = Effect(state, Action.Callback(action))
