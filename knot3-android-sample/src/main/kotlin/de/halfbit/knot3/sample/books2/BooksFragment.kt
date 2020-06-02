package de.halfbit.knot3.sample.books2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.halfbit.knot3.sample.R
import de.halfbit.knot3.sample.common.ViewBinder
import io.reactivex.rxjava3.disposables.CompositeDisposable

class BooksFragment : Fragment() {

    private val disposable = CompositeDisposable()
    private lateinit var viewBinder: ViewBinder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_books, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinder = BooksViewBinder(DefaultBookView(view), getViewModel())
    }

    override fun onStart() {
        super.onStart()
        viewBinder.bind(disposable)
    }

    override fun onStop() {
        disposable.clear()
        super.onStop()
    }
}

private inline fun <reified VM : ViewModel> Fragment.getViewModel(): VM =
    ViewModelProvider(this, BooksViewModelFactory).get(VM::class.java)

private object BooksViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        if (modelClass.isAssignableFrom(BooksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            DefaultBooksViewModel() as T
        } else error("Unsupported model type: $modelClass")
}
