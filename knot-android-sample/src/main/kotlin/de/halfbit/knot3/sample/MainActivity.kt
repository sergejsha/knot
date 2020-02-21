package de.halfbit.knot3.sample

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        val model = ViewModelProvider(this, mainViewModelFactory).get(MainViewModel::class.java)

        model.showButton.subscribe {
            loadingButton.visibility = if (it) VISIBLE else GONE
        }.also { disposable.add(it) }

        model.showLoading.subscribe {
            val visibility = if (it) VISIBLE else GONE
            loadingIndicator.visibility = visibility
            hint.visibility = visibility
        }.also { disposable.add(it) }

        model.showMovies.subscribe {
            movies.text = "Movies:\n${it.joinToString(separator = "\n")}"
        }.also { disposable.add(it) }

        loadingButton.setOnClickListener { model.onButtonClick() }
    }

    override fun onStop() {
        disposable.clear()
        super.onStop()
    }
}