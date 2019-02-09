package de.halfbit.knot.dsl

@KnotDsl
class StateBuilder<State : Any>
internal constructor() {
    var initial: State? = null
}