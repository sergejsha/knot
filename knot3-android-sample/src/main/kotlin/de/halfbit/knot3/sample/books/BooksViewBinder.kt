package de.halfbit.knot3.sample.books

import de.halfbit.knot3.sample.books.model.State
import de.halfbit.knot3.sample.common.ViewBinder
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.plusAssign

internal class BooksViewBinder(
    private val view: BooksView,
    private val viewModel: BooksViewModel
) : ViewBinder {

    override fun bind(disposable: CompositeDisposable) {
        view.bindEvents(disposable)
        viewModel.bindState(disposable)
    }

    private fun BooksView.bindEvents(disposable: CompositeDisposable) {
        disposable += event.subscribe(viewModel.event)
    }

    private fun BooksViewModel.bindState(disposable: CompositeDisposable) {
        disposable += state
            .ofType<State.Loading>()
            .subscribe { view.showLoading() }

        disposable += state
            .ofType<State.Empty>()
            .subscribe { view.showEmpty() }

        disposable += state
            .ofType<State.Content>()
            .map { it.books }
            .subscribe(view::showBooks)

        disposable += state
            .ofType<State.Error>()
            .map { it.message }
            .subscribe(view::showError)
    }
}
