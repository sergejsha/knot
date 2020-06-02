package de.halfbit.knot3.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.halfbit.knot3.sample.books2.BooksFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.rootFrameLayout, BooksFragment())
                .commit()
        }
    }
}
