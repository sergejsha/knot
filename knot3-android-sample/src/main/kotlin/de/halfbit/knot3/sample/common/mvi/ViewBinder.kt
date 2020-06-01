package de.halfbit.knot3.sample.common.mvi

import io.reactivex.rxjava3.disposables.CompositeDisposable

interface ViewBinder {
    fun bind(disposable: CompositeDisposable)
}
