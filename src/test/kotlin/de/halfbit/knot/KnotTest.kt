package de.halfbit.knot

class KnotTest {

    /*
    private lateinit var knot: Knot<State, Command, Change>

    private lateinit var with: With<State, Command>
    private val changeTransformer: ToChangeTransformer<State, Command, Change> = mock {
        on { invoke(any()) }.thenAnswer { invocation ->
            invocation.arguments?.let { arguments ->
                @Suppress("UNCHECKED_CAST")
                with = arguments[0] as With<State, Command>
            }
            Observable.just(Change)
        }
    }

    private val commandTransformer: ToCommandTransformer<State, Command> = mock {
        on { invoke(any()) }.thenAnswer { invocation ->
            invocation.arguments?.let { arguments ->
                @Suppress("UNCHECKED_CAST")
                with = arguments[0] as With<State, Command>
            }
            Observable.just(Change)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `DSL requires initial state`() {
        knot = knot { }
    }

    @Test(expected = IllegalStateException::class)
    fun `DSL requires reducer`() {
        knot = knot {
            state {
                initial = State()
            }
        }
    }

    @Test
    fun `DSL builder creates Knot`() {
        knot = knot {
            state {
                initial = State()
                reduce { state, _ -> state }
            }
        }
        assertThat(knot).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'state'`() {
        knot = knot {
            state {
                initial = State()
                reduce { state, _ -> state }
            }
        }
        assertThat(knot.state).isNotNull()
    }

    @Test
    fun `DSL builder creates Knot with 'command'`() {
        knot = knot {
            state {
                initial = State()
                reduce { state, _ -> state }
            }
        }
        assertThat(knot.command).isNotNull()
    }

    @Test
    fun `Knot dispatches initial state`() {
        val state = State()
        knot = knot {
            state {
                initial = state
                reduce { state, _ -> state }
            }
        }
        val observer = knot.state.test()
        observer.assertValue(state)
    }

    @Test
    fun `Knot contains initial state`() {
        val state = State()
        knot = knot {
            state {
                initial = state
                reduce { state, _ -> state }
            }
        }
        assertThat(knot.currentState).isEqualTo(state)
    }

    @Test
    fun `Knot reduces currentState`() {
        knot = knot {
            state {
                initial = State()
                reduce { state, change -> State(value = 1) }
            }
            issueChange {
                command.map { Change }
            }
        }
        knot.command.accept(Command)
        assertThat(knot.currentState).isEqualTo(State(1))
    }

    @Test
    fun `Knot invokes changeTransformers`() {
        knot = knot {
            state {
                initial = State()
                reduce { state, _ -> state }
            }
            issueChange(changeTransformer)
        }
        verify(changeTransformer).invoke(any())
    }

    @Test
    fun `Knot provides state into changeTransformers`() {
        knot = knot {
            state {
                initial = State()
                reduce { state, _ -> state }
            }
            issueChange(changeTransformer)
        }
        assertThat(with.state).isNotNull()
    }

    @Test
    fun `Knot provides command into changeTransformers`() {
        knot = knot {
            state {
                initial = State()
                reduce { state, _ -> state }
            }
            issueChange(changeTransformer)
        }
        assertThat(with.command).isNotNull()
    }

    @Test
    fun `Knot invokes commandTransformers`() {
        //val observer: ToChangeTransformer<State, Command, Change> = mock()
        knot = knot {
            state {
                initial = State()
                reduce { state, _ -> state }
            }
            issueCommand(commandTransformer)
        }
        verify(commandTransformer).invoke(any())
    }

    @Test
    fun `Knot provides state into commandTransformers`() {
        knot = knot {
            state {
                initial = State()
                reduce { state, _ -> state }
            }
            issueCommand(commandTransformer)
        }
        assertThat(with.state).isNotNull()
    }

    @Test
    fun `Knot provides command into commandTransformers`() {
        knot = knot {
            state {
                initial = State()
                reduce { state, _ -> state }
            }
            issueCommand(commandTransformer)
        }
        assertThat(with.command).isNotNull()
    }

    private data class State(val value: Int = 0)
    private object Command
    private object Change

    */

}

