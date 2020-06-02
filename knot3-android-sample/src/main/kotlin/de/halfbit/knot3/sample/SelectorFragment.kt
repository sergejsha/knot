package de.halfbit.knot3.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

typealias OrdinaryKnotFragment = de.halfbit.knot3.sample.books.BooksFragment
typealias CompositeKnotFragment = de.halfbit.knot3.sample.books2.BooksFragment

class SelectorFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_selector, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.books).setOnClickListener {
            navigateTo(OrdinaryKnotFragment())
        }

        view.findViewById<View>(R.id.books2).setOnClickListener {
            navigateTo(CompositeKnotFragment())
        }
    }

    private fun navigateTo(fragment: Fragment) {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.rootFrameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }
}