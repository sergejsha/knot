package de.halfbit.knot.dsl

import io.reactivex.Scheduler

@KnotDsl
class OnState<State : Any>
internal constructor() {
    var initial: State? = null
    val reduceOn: Scheduler? = null
}