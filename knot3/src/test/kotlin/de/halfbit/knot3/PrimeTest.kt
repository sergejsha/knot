package de.halfbit.knot3

import com.google.common.truth.Truth.assertThat
import de.halfbit.knot3.utils.RxPluginsException
import de.halfbit.knot3.utils.SchedulerTester
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Rule
import org.junit.Test

class PrimeTest {

    @Rule
    @JvmField
    var rxPluginsException: RxPluginsException = RxPluginsException.none()

    private data class State(val value: String)

    private sealed class Change(val value: String) {
        object A : Change("a")
        object ADone : Change("a-done")
        object B : Change("b")
        object BDone : Change("b-done")
    }

    private sealed class Action {
        object A : Action()
        object B : Action()
    }

    @Test
    fun `Exception thrown if reducer cannot be found when no state observers`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        rxPluginsException.expect(IllegalStateException::class)
        knot.compose()
        knot.change.accept(Change.A)
    }

    @Test
    fun `Exception thrown if reducer cannot be found when one state observer`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        rxPluginsException.expect(IllegalStateException::class)
        knot.state.test()
        knot.compose()
        knot.change.accept(Change.A)
    }

    @Test
    fun `CompositeKnot picks reducers by change type`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { copy(value = it.value).only }
                reduce<Change.B> { copy(value = it.value).only }
            }
        }

        val observer = knot.state.test()
        knot.compose()

        knot.change.accept(Change.A)
        knot.change.accept(Change.B)

        observer.assertValues(
            State("empty"),
            State("a"),
            State("b")
        )
    }

    @Test
    fun `CompositeKnot transforms events into changes`() {
        val changeA = PublishSubject.create<Unit>()
        val changeB = PublishSubject.create<Unit>()

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { copy(value = it.value).only }
                reduce<Change.B> { copy(value = it.value).only }
            }
            events {
                source { changeA.map { Change.A } }
                source { changeB.map { Change.B } }
            }
        }

        val observer = knot.state.test()
        knot.compose()

        changeA.onNext(Unit)
        changeB.onNext(Unit)

        observer.assertValues(
            State("empty"),
            State("a"),
            State("b")
        )
    }

    @Test
    fun `CompositeKnot performs actions`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { copy(value = it.value) + Action.A }
                reduce<Change.ADone> { copy(value = it.value).only }
                reduce<Change.B> { copy(value = it.value) + Action.B }
                reduce<Change.BDone> { copy(value = it.value).only }
            }
            actions {
                perform<Action.A> { map { Change.ADone } }
                perform<Action.B> { map { Change.BDone } }
            }
        }

        val observer = knot.state.test()
        knot.compose()

        knot.change.accept(Change.A)
        knot.change.accept(Change.B)

        observer.assertValues(
            State("empty"),
            State("a"),
            State("a-done"),
            State("b"),
            State("b-done")
        )
    }

    @Test
    fun `changes { reduceOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from {
            visited = true
            it.run()
        }
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            changes {
                reduceOn = scheduler
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { only }
            }
        }

        knot.compose()
        knot.state.test()
        knot.change.accept(Change.A)

        assertThat(visited).isTrue()
    }

    @Test
    fun `changes { watchOn } gets applied`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
            changes {
                watchOn = schedulerTester.scheduler("one")
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { only }
            }
        }

        knot.compose()
        knot.state.test()
        knot.change.accept(Change.A)

        schedulerTester.assertSchedulers("one")
    }

    @Test
    fun `changes { watchOn } is null by default`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
            changes {
                assertThat(watchOn).isNull()
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { only }
            }
        }
        knot.compose()
    }

    @Test
    fun `changes { watchOn } gets applied before each watcher`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
            changes {
                watchOn = schedulerTester.scheduler("one")
                watchAll { }
                watchOn = schedulerTester.scheduler("two")
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { only }
            }
        }
        knot.compose()
        knot.state.test()
        knot.change.accept(Change.A)

        schedulerTester.assertSchedulers("one", "two")
    }

    @Test
    fun `Disposed CompositeKnot ignores emitted changes`() {

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { copy(value = it.value).only }
            }
        }

        val observer = knot.state.test()
        knot.compose()
        knot.dispose()

        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty")
        )
    }

    @Test
    fun `Disposed CompositeKnot ignores emitted events`() {

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        val eventSource = PublishSubject.create<Unit>()
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { copy(value = it.value).only }
            }
            events {
                source {
                    eventSource.map { Change.A }
                }
            }
        }

        val observer = knot.state.test()
        knot.compose()
        knot.dispose()

        eventSource.onNext(Unit)

        observer.assertValues(
            State("empty")
        )
    }

    @Test
    fun `Composed CompositeKnot subscribes event source`() {

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        val eventSource = PublishSubject.create<Unit>()
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { only }
            }
            events {
                source {
                    eventSource.map { Change.A }
                }
            }
        }

        knot.compose()
        assertThat(eventSource.hasObservers()).isTrue()
    }

    @Test
    fun `Disposed CompositeKnot unsubscribes event source`() {

        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        val eventSource = PublishSubject.create<Unit>()
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change> { only }
            }
            events {
                source {
                    eventSource.map { Change.A }
                }
            }
        }

        knot.compose()
        knot.dispose()
        assertThat(eventSource.hasObservers()).isFalse()
    }

    @Test
    fun `Exception throw in reducer gets propagated when no state observers`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { unexpected(it) }
            }
        }

        rxPluginsException.expect(IllegalStateException::class)
        knot.compose()
        knot.change.accept(Change.A)
    }

    @Test
    fun `Exception throw in reducer gets propagated when one state observer`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }

        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { unexpected(it) }
            }
        }

        rxPluginsException.expect(IllegalStateException::class)
        knot.state.test()
        knot.compose()
        knot.change.accept(Change.A)
    }

    @Test
    fun `Prime actions { watchAll } receives Action`() {
        val watcher = PublishSubject.create<Action>()
        val observer = watcher.test()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                watchAll { watcher.onNext(it) }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Action.A
        )
    }

    @Test
    fun `Prime actions { watch } receives Action`() {
        val watcher = PublishSubject.create<Action>()
        val observer = watcher.test()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                watch<Action.A> { watcher.onNext(it) }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Action.A
        )
    }

    @Test
    fun `Prime actions { watchOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from { visited = true; it.run() }
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                watchOn = scheduler
                watchAll { }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        assertThat(visited).isTrue()
    }

    @Test
    fun `Prime actions { watchOn } is null by default`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                assertThat(watchOn).isNull()
                watchAll { }
            }
        }
        knot.compose()
    }

    @Test
    fun `Prime actions { watchOn } gets applied before each watcher`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                watchOn = schedulerTester.scheduler("one")
                watchAll { }
                watchOn = schedulerTester.scheduler("two")
                watchAll { }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        schedulerTester.assertSchedulers("one", "two")
    }

    @Test
    fun `Prime actions { intercept } receives Action`() {
        val watcher = PublishSubject.create<Action>()
        val observer = watcher.test()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                intercept { action -> action.doOnNext { watcher.onNext(it) } }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Action.A
        )
    }

    @Test
    fun `Prime changes { watchAll } receives Change`() {
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
                watchAll { watcher.onNext(it) }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Change.A
        )
    }

    @Test
    fun `Prime changes { watch } receives Change`() {
        val watcher = PublishSubject.create<Change>()
        val observer = watcher.test()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
                watch<Change.A> { watcher.onNext(it) }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        observer.assertValues(
            Change.A
        )
    }

    @Test
    fun `Prime changes { watchOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from { visited = true; it.run() }
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
                watchOn = scheduler
                watchAll { }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        assertThat(visited).isTrue()
    }

    @Test
    fun `Prime changes { watchOn } is null by default`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
                assertThat(watchOn).isNull()
                watchAll { }
            }
        }
        knot.compose()
    }

    @Test
    fun `Prime changes { watchOn } gets applied before each watcher`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
                watchOn = schedulerTester.scheduler("one")
                watchAll { }
                watchOn = schedulerTester.scheduler("two")
                watchAll { }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        schedulerTester.assertSchedulers("one", "two")
    }

    @Test
    fun `Prime state { intercept } intercepts after distinctUntilChanged`() {
        val interceptor = PublishSubject.create<State>()
        val observer = interceptor.test()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
                intercept {
                    it.doOnNext { state -> interceptor.onNext(state) }
                }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> {
                    if (value == "empty") State("changed").only else only
                }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        knot.change.accept(Change.A)
        observer.assertValues(
            State("empty"),
            State("changed")
        )
    }

    @Test
    fun `CompositeKnot state { watchAll } receives State`() {
        val watcher = PublishSubject.create<State>()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
                watchAll { watcher.onNext(it) }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `CompositeKnot state { watchOn } gets applied`() {
        var visited = false
        val scheduler = Schedulers.from { visited = true; it.run() }
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
                watchOn = scheduler
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one").only }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        assertThat(visited).isTrue()
    }

    @Test
    fun `CompositeKnot state { watchOn } is null by default`() {
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
                assertThat(watchOn).isNull()
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one").only }
            }
        }
        knot.compose()
    }

    @Test
    fun `CompositeKnot state { watchOn } gets applied before each watcher`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
                watchOn = schedulerTester.scheduler("one")
                watchAll { }
                watchOn = schedulerTester.scheduler("two")
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one").only }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        schedulerTester.assertSchedulers("one", "two")
    }

    @Test
    fun `CompositeKnot actions { watchOn } in knot gets applied in knot`() {
        var visited = false
        val scheduler = Schedulers.from { visited = true; it.run() }
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            actions {
                watchOn = scheduler
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        assertThat(visited).isTrue()
    }

    @Test
    fun `CompositeKnot actions { watchOn } in knot gets applied in prime`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            actions {
                watchOn = schedulerTester.scheduler("knot")
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                watchAll { }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        schedulerTester.assertSchedulers("knot")
    }

    @Test
    fun `CompositeKnot actions { watchOn } in knot can be overrided in prime`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            actions {
                watchOn = schedulerTester.scheduler("knot")
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
            actions {
                watchOn = schedulerTester.scheduler("prime")
                watchAll { }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        schedulerTester.assertSchedulers("knot", "prime")
    }

    @Test
    fun `CompositeKnot actions { watchOn } is null by defaul`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
            actions {
                assertThat(watchOn).isNull()
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
        }
        knot.compose()
    }

    @Test
    fun `CompositeKnot actions { watchOn } gets applied before each watcher`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            actions {
                watchOn = schedulerTester.scheduler("one")
                watchAll { }
                watchOn = schedulerTester.scheduler("two")
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        schedulerTester.assertSchedulers("one", "two")
    }

    @Test
    fun `CompositeKnot changes { watchAll } receives Change`() {
        val watcher = PublishSubject.create<Any>()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            changes {
                watchAll { watcher.onNext(it) }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            Change.A
        )
    }

    @Test
    fun `CompositeKnot changes { watchOn } in knot gets applied in knot`() {
        var visited = false
        val scheduler = Schedulers.from { visited = true; it.run() }
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            changes {
                watchOn = scheduler
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { only }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        assertThat(visited).isTrue()
    }

    @Test
    fun `CompositeKnot changes { watchOn } in knot gets applied in prime`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            changes {
                watchOn = schedulerTester.scheduler("one")
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { only }
                watchAll { }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        schedulerTester.assertSchedulers("one")
    }

    @Test
    fun `CompositeKnot changes { watchOn } is null by default`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
            changes {
                assertThat(watchOn).isNull()
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { only }
            }
        }
        knot.compose()
    }

    @Test
    fun `CompositeKnot changes { watchOn } gets applied before each watcher`() {
        val schedulerTester = SchedulerTester()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            changes {
                watchOn = schedulerTester.scheduler("one")
                watchAll { }
                watchOn = schedulerTester.scheduler("two")
                watchAll { }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { this + Action.A }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        schedulerTester.assertSchedulers("one", "two")
    }

    @Test
    fun `CompositeKnot actions { watchAll } receives Action`() {
        val watcher = PublishSubject.create<Any>()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
            actions {
                watchAll { watcher.onNext(it) }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one") + Action.A }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            Action.A
        )
    }

    @Test
    fun `CompositeKnot state { watch } receives State`() {
        val watcher = PublishSubject.create<State>()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
                watch<State> { watcher.onNext(it) }
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `CompositeKnot filters same state before dispatching`() {
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> {
                    if (value == "empty") State("one").only else only
                }
            }
        }

        val observer = knot.state.test()
        knot.compose()
        knot.change.accept(Change.A)
        knot.change.accept(Change.A)
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `CompositeKnot dispatches equal but not the same state`() {
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> {
                    State("one").only
                }
            }
        }

        val observer = knot.state.test()
        knot.compose()
        knot.change.accept(Change.A)
        knot.change.accept(Change.A)
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one"),
            State("one"),
            State("one")
        )
    }

    @Test
    fun `Prime state { watchAll } receives State`() {
        val watcher = PublishSubject.create<State>()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
        }
        knot.registerPrime<Change, Action> {
            state {
                watchAll { watcher.onNext(it) }
            }
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `Prime state { watch } receives State`() {
        val watcher = PublishSubject.create<State>()
        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
        }
        knot.registerPrime<Change, Action> {
            state {
                watch<State> { watcher.onNext(it) }
            }
            changes {
                reduce<Change.A> { State("one").only }
            }
        }

        val observer = watcher.test()
        knot.compose()
        knot.change.accept(Change.A)

        observer.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `Prime receives updates when listens to state updates inside events { } section`() {

        val knot = compositeKnot<State> {
            state {
                initial = State("empty")
            }
        }

        val stateObserver = knot.state.test()
        knot.registerPrime<Change, Unit> {
            changes {
                reduce<Change.A> {
                    State("one").only
                }
            }
            events {
                source {
                    knot.state
                        .filter { it.value == "empty" }
                        .map { Change.A }
                }
            }
        }
        knot.compose()

        stateObserver.assertValues(
            State("empty"),
            State("one")
        )
    }

    @Test
    fun `isDisposed returns false if not has not been disposed`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Unit> {
            changes { reduce<Change> { only } }
        }
        assertThat(knot.isDisposed).isFalse()
    }

    @Test
    fun `isDisposed returns true if Knot has been disposed`() {
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Unit> {
            changes { reduce<Change> { only } }
        }
        knot.compose()
        knot.dispose()
        assertThat(knot.isDisposed).isTrue()
    }

    @Test
    fun `Disposed Knot disposes events`() {
        val events = PublishSubject.create<Unit>()
        var isDisposed = false
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes { reduce<Change.A> { only } }
            events {
                source {
                    events
                        .doOnDispose { isDisposed = true }
                        .map { Change.A }
                }
            }
        }
        knot.compose()
        knot.dispose()
        assertThat(isDisposed).isTrue()
    }

    @Test
    fun `Disposed Knot disposes actions`() {
        val actions = PublishSubject.create<Unit>()
        var isDisposed = false
        val knot = compositeKnot<State> {
            state { initial = State("empty") }
        }
        knot.registerPrime<Change, Action> {
            changes {
                reduce<Change.A> {
                    this + Action.A
                }
            }
            actions {
                perform<Action.A> {
                    actions
                        .doOnDispose { isDisposed = true }
                        .map { Change.A }
                }
            }
        }
        knot.compose()
        knot.change.accept(Change.A)
        knot.dispose()

        assertThat(isDisposed).isTrue()
    }
}