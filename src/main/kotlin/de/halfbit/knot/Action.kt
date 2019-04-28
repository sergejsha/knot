package de.halfbit.knot

import io.reactivex.Completable as RxCompletable
import io.reactivex.Maybe as RxMaybe
import io.reactivex.Single as RxSingle

sealed class Action<Change : Any> {
    class Single<Change : Any>(val actual: RxSingle<Change>) : Action<Change>()
    class Maybe<Change : Any>(val actual: RxMaybe<Change>) : Action<Change>()
    class Completable<Change : Any>(val actual: RxCompletable) : Action<Change>()
    class Callback<Change : Any>(val actual: () -> Unit) : Action<Change>()
}
