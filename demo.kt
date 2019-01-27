
interface BookService {

    sealed class State {
        object Idling : State()
        class Loading : State()
        class Content(val books: List<Book>) : State()
        class Error(var error: Throwable) : State()
    }

    sealed class Command {
        object Load : Command()
        class Create(book: Book) : Command()
        class Delete(book: Book) : Command()
    }

    val state: Observable<State>
    val command: Consumer<Command>
}


class DefaultBookService(
    private val api: BookServiceApi,
    private val profileRepository: ProfileRepository
) : BookService {

    private val statechart = statechart<State, Command> {

        on<Command.Load> { to(State.Loading) }
        on<Command.Load, Command.Cancel> { loadOrCancelBooks() }
        on<Command.Create> { createBook() }
        on(profileRepository.profile) { reloadProfile() }
        onSubscribe { post(Command.Load) }

        any { state ->
            on<Command.Load> { to(State.Loading) }
            on<Command.Load, Command.Cancel> { loadOrCancelBooks() }
            on<Command.Create> { createBook() }
            on(profileRepository.profile) { reloadProfile() }
            onSubscribe { post(Command.Load) }
        }

        state<State.Content> {
            on<Command.LoadNextPage> { to(State.Loading) }
            on<Command.LoadNextPage> { loadNextPage() }
        }

        state<State.Error> { }
    }

    override val state = statechart.state
    override val command = statechart.command

    private fun Observable<Pair<Command, State>>.loadOrCancelBooks() =
        switchMap { command, state ->
            when(command) {
                Command.Load -> api.getBooks()
                Command.Cancel -> Single.never()
                else -> unsupported()
            }
        }
        .map<State> {
            when (it) {
                is BooksResponse.Success -> State.Content(it.books)
                is BooksResponse.Failure -> State.Error(it.error)
            }
        }

    private fun createBook(command: Observable<Command>, state: State): Observable<State> =
        switchMap { 
            api.createBook(it.book) 
        }
        .switchMapSingle<State> {
            when (it) {
                is BookCreateResponse.Success -> mutate(state)
                is BookCreateResponse.Failure -> Observable.never()
            }
        }        

    private Observable<Command.LoadNextPage>.loadNextPage(): Observable<State> = 
        switchMap { command, state -> 
            api.loadNextPage(state.currentPage + 1)
                .withLatestFrom(state)
        }
        .switchMapSingle<State> {
            when (it) {
                is BookCreateResponse.Success -> onPageLoaded(it)
                is BookCreateResponse.Failure -> Observable.never()
            }
        }        


}


