package de.halfbit.knot3.utils

import com.google.common.truth.Truth.assertThat
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.Executor

class SchedulerTester {
    private val observedSchedulers = mutableListOf<String>()

    fun scheduler(name: String): Scheduler = Schedulers.from(IndexedExecutor(name))

    fun assertSchedulers(vararg schedulers: String) {
        assertThat(observedSchedulers).containsAtLeastElementsIn(schedulers.toMutableList())
    }

    private inner class IndexedExecutor(val name: String) : Executor {
        override fun execute(command: Runnable) {
            observedSchedulers.add(name)
            command.run()
        }
    }
}
