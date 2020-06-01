package de.halfbit.knot3.sample.common.actions

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

interface LoadBooksAction {
    fun perform(): Single<Result>

    sealed class Result {
        data class Success(val books: List<Book>) : Result()
        sealed class Failure : Result() {
            object Network : Failure()
            object Generic : Failure()
        }
    }

    data class Book(val title: String, val year: String)
}

internal class DefaultLoadBooksAction(
    private val ioScheduler: Scheduler = Schedulers.io()
) : LoadBooksAction {
    override fun perform(): Single<LoadBooksAction.Result> =
        Single.just(books)
            .delay(4, TimeUnit.SECONDS)
            .subscribeOn(ioScheduler)
            .map { LoadBooksAction.Result.Success(it) }
}

private val books = listOf(
    LoadBooksAction.Book("The Hobbit or There and Back Again", "1937"),
    LoadBooksAction.Book("Leaf by Niggle", "1945"),
    LoadBooksAction.Book("The Lay of Aotrou and Itroun", "1945"),
    LoadBooksAction.Book("Farmer Giles of Ham", "1949"),
    LoadBooksAction.Book("The Homecoming of Beorhtnoth Beorhthelm's Son", "1953"),
    LoadBooksAction.Book("The Lord of the Rings - The Fellowship of the Ring", "1954"),
    LoadBooksAction.Book("The Lord of the Rings - The Two Towers", "1954"),
    LoadBooksAction.Book("The Lord of the Rings - The Return of the King", "1955"),
    LoadBooksAction.Book("The Adventures of Tom Bombadil", "1962"),
    LoadBooksAction.Book("Tree and Leaf", "1964"),
    LoadBooksAction.Book("The Tolkien Reader", "1966"),
    LoadBooksAction.Book("The Road Goes Ever On", "1967"),
    LoadBooksAction.Book("Smith of Wootton Major", "1967")
)
