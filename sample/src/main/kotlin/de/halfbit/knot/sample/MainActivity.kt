package de.halfbit.knot.sample

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val model = ViewModelProvider(this).get(MainViewModel::class.java)

        model.showButton.subscribe {
            loadingButton.visibility = if (it) VISIBLE else GONE
        }.also { disposable.add(it) }

        model.showLoading.subscribe {
            loadingIndicator.visibility = if (it) VISIBLE else GONE
        }.also { disposable.add(it) }

        model.showMovies.subscribe {
            movies.text = "Movies:\n${it.joinToString(separator = "\n")}"
        }.also { disposable.add(it) }

        loadingButton.setOnClickListener { model.onButtonClick() }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}