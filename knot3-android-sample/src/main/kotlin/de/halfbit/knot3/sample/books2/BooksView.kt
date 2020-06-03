package de.halfbit.knot3.sample.books2

import android.view.View
import android.widget.TextView
import de.halfbit.knot3.sample.R
import de.halfbit.knot3.sample.books2.model.Event
import de.halfbit.knot3.sample.books2.model.types.Book
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

interface BooksView {
    val event: Observable<Event>

    fun showEmpty()
    fun showLoading()
    fun showBooks(books: List<Book>)
    fun showError(message: String)
}

internal class DefaultBookView(rootView: View) : BooksView {

    private val pageEmpty: View = rootView.findViewById(R.id.pageEmpty)
    private val pageLoading: View = rootView.findViewById(R.id.pageLoading)
    private val pageContent: View = rootView.findViewById(R.id.pageContent)
    private val pageError: View = rootView.findViewById(R.id.pageError)
    private val booksMessage: TextView = rootView.findViewById(R.id.booksMessage)
    private val errorMessage: TextView = rootView.findViewById(R.id.errorMessage)

    init {
        val loadListener = View.OnClickListener { event.onNext(Event.Load) }
        rootView.findViewById<View>(R.id.tryAgainButton).setOnClickListener(loadListener)
        rootView.findViewById<View>(R.id.reloadButton).setOnClickListener(loadListener)
        rootView.findViewById<View>(R.id.loadButton).setOnClickListener(loadListener)

        val clearListener = View.OnClickListener { event.onNext(Event.Clear) }
        rootView.findViewById<View>(R.id.clearButton).setOnClickListener(clearListener)
    }

    override val event: Subject<Event> = PublishSubject.create()

    override fun showEmpty() {
        pageEmpty.show()
    }

    override fun showLoading() {
        pageLoading.show()
    }

    override fun showBooks(books: List<Book>) {
        pageContent.show()
        val text = books.joinToString(separator = "\n") {
            "${it.title} (${it.year})"
        }
        booksMessage.text = "Books:\n$text"
    }

    override fun showError(message: String) {
        pageError.show()
        errorMessage.text = message
    }

    private fun View.show() {
        if (visibility != View.VISIBLE) {
            visibility = View.VISIBLE
        }
        if (this != pageContent) pageContent.hide()
        if (this != pageLoading) pageLoading.hide()
        if (this != pageError) pageError.hide()
        if (this != pageEmpty) pageEmpty.hide()
    }

    private fun View.hide() {
        if (visibility != View.INVISIBLE) {
            visibility = View.INVISIBLE
        }
    }
}
