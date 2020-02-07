package de.halfbit.knot.sample

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

internal fun createLoadAction(
    delayScheduler: Scheduler = Schedulers.computation()
): (Observable<Action.Load>) -> Observable<Change> = {
    it
        .delay(5, TimeUnit.SECONDS, delayScheduler) // To fake the loading
        .map {
            // Do an operation to load the movies
            listOf(
                Movie("The day after tomorrow"),
                Movie("Joker"),
                Movie("Avatar")
            )
        }
        .map { movies -> Change.Load.Success(movies) as Change }
        .onErrorReturn { Change.Load.Failure }
}