package de.halfbit.knot.sample

import io.reactivex.Observable
import java.util.concurrent.TimeUnit

val loadActionFactory: (Observable<Action.Load>) -> Observable<Change> = {
    it
        .delay(5, TimeUnit.SECONDS) // To fake the loading
        .map {
            // Do an operation to load the movies
            listOf(
                Movie("The day after tomorrow"),
                Movie("Joker"),
                Movie("Avatar")
            )
        }
        .map { movies -> Change.Load.Success(movies) as Change }
        .onErrorReturn { Change.Load.Fail }
}