package de.halfbit.knot3.sample.books

import android.view.View
import android.widget.TextView
import de.halfbit.knot3.sample.R
import de.halfbit.knot3.sample.books.model.Event
import de.halfbit.knot3.sample.books.model.types.Book
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

interface BooksView {
    val event: Observable<Event>

    fun showLoading(loading: Boolean)
    fun showBooks(books: List<Book>)
    fun showError(message: String)
}

internal class DefaultBookView(rootView: View) : BooksView {

    private val pageInitial: View = rootView.findViewById(R.id.pageInitial)
    private val pageLoading: View = rootView.findViewById(R.id.pageLoading)
    private val pageContent: View = rootView.findViewById(R.id.pageContent)
    private val pageError: View = rootView.findViewById(R.id.pageError)
    private val booksMessage: TextView = rootView.findViewById(R.id.booksMessage)
    private val errorMessage: TextView = rootView.findViewById(R.id.errorMessage)

    init {
        val onClickListener = View.OnClickListener { event.onNext(Event.Refresh) }
        rootView.findViewById<View>(R.id.tryAgainButton).setOnClickListener(onClickListener)
        rootView.findViewById<View>(R.id.reloadButton).setOnClickListener(onClickListener)
        rootView.findViewById<View>(R.id.loadButton).setOnClickListener(onClickListener)
    }

    override val event: Subject<Event> = PublishSubject.create()

    override fun showLoading(loading: Boolean) {
        if (loading) Page.Loading.show()
        else Page.Loading.hide()
    }

    override fun showBooks(books: List<Book>) {
        Page.Content.show()
        val text = books.joinToString(separator = "\n") {
            "${it.title} (${it.year})"
        }
        booksMessage.text = "Books:\n$text"
    }

    override fun showError(message: String) {
        Page.Error.show()
        errorMessage.text = message
    }

    private fun Page.show() {
        when (this) {
            Page.Loading -> {
                pageLoading.show()
                pageContent.hide()
                pageError.hide()
                pageInitial.hide()
            }
            Page.Content -> {
                pageLoading.hide()
                pageContent.show()
                pageError.hide()
                pageInitial.hide()
            }
            Page.Error -> {
                pageLoading.hide()
                pageContent.hide()
                pageError.show()
                pageInitial.hide()
            }
        }
    }

    private fun Page.hide() {
        when (this) {
            Page.Loading -> pageLoading.hide()
            Page.Content -> pageContent.hide()
            Page.Error -> pageError.hide()
        }
    }

    private fun View.show() {
        if (visibility != View.VISIBLE) {
            visibility = View.VISIBLE
        }
    }

    private fun View.hide() {
        if (visibility != View.GONE) {
            visibility = View.INVISIBLE
        }
    }
}

private enum class Page { Loading, Content, Error }
