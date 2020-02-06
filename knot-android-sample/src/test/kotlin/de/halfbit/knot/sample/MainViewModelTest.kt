package de.halfbit.knot.sample

import com.google.common.truth.Truth
import io.reactivex.schedulers.TestScheduler
import org.junit.Test

class MainViewModelTest {

    private lateinit var changeLoadResult: Change
    private val stateObserver = TestScheduler()
    private val mockKnot = mainKnotFactory(stateObserver, { it.map { changeLoadResult } })
    private val model = MainViewModel(mockKnot)
    private val buttonShowObserver = model.showButton.test()
    private val loadingShowObserver = model.showLoading.test()
    private val moviesShowObserver = model.showMovies.test()

    @Test
    fun `on button click with loading fails should emit correct values`() {
        changeLoadResult = Change.Load.Fail

        model.onButtonClick()

        stateObserver.triggerActions()
        Truth.assertThat(buttonShowObserver.values()).containsAtLeastElementsIn(arrayOf(true, false))
        Truth.assertThat(loadingShowObserver.values()).containsAtLeastElementsIn(arrayOf(false, true, false))
        moviesShowObserver.assertNoValues()
    }

    @Test
    fun `on button click with loading success should emit correct values`() {
        val moviesList = listOf(Movie("Batman Begins"))
        changeLoadResult = Change.Load.Success(moviesList)

        model.onButtonClick()

        stateObserver.triggerActions()
        Truth.assertThat(buttonShowObserver.values()).containsAtLeastElementsIn(arrayOf(true, false))
        Truth.assertThat(loadingShowObserver.values()).containsAtLeastElementsIn(arrayOf(false, true, false))
        Truth.assertThat(moviesShowObserver.values()).contains(moviesList)
    }
}
