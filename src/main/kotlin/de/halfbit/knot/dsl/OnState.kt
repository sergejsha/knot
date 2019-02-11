package de.halfbit.knot.dsl

@KnotDsl
class OnState<State : Any>
internal constructor() {
    var initial: State? = null
}