package de.halfbit.knot3.sample.common

import io.reactivex.rxjava3.disposables.CompositeDisposable

interface ViewBinder {
    fun bind(disposable: CompositeDisposable)
}
