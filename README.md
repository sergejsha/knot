# Knot

[![Build Status](https://travis-ci.org/beworker/knot.svg?branch=master)](https://travis-ci.org/beworker/knot)
[![Maven Central](http://img.shields.io/maven-central/v/de.halfbit/knot.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.halfbit%22%20a%3A%22knot%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Concise reactive state container library for Android applications.

# Why Knot?

* Predictable - state is the single source of truth.
* Side-effect free reducer - by desing.
* Scalable - single knots can be combined together to build more complex application logic.
* Decomposable - complex knots can be decomposed into primes by related functionality.
* Structured - easy to read and write declarative API for writing better structured and less buggy code.
* Concise - it has minimalistic API and compact implementation.
* Testable - reducers and transformers are easy to test. 
* Why not?

# Concept

Knot helps managing application state by reacting on events and performing asynchronous actions in a structured way. There are five core concepts Knot defines: `State`, `Change`, `Reducer`, `Effect` and `Action`.

<img src="docs/diagrams/flowchart-knot.png" width="490" />

`State` represents an immutable partial state of an Android application. It can be a state of a screen or a state of an internal headless component, like repository.

`Change` is an immutable data object with an optional payload intended for changing the `State`. A `Change` can be produced from an external event or be a result of execution of an `Action`.

`Action` is a synchronous or an asynchronous operation which, when completed, can emit a new `Change`.

`Reducer` is a function that takes the previous `State` and a `Change` as arguments and returns the new `State` and an optional `Action` wrapped by `Effect` class. `Reducer` in Knot is designer to stays side-effects free because each side-effect can be turned into an `Action` and returned from reducer function together with a new state.

`Effect` is a convenient wrapper class containing the new `State` and an optional `Action`. If `Action` is present, Knot will perform it and provide resulting `Change` back to `Reducer`.

# Getting Started

The example below declares a Knot capable of loading data, handling *Success* and *Failure* loading results and reloading data automatically when an external *"data changed"* signal gets received. It also writes all `State` mutations as well as all processed `Changes` and `Actions` in console.

```kotlin
sealed class State {
   object Empty : State()
   object Loading : State()
   data class Content(val data: String): State()
   data class Failed(val error: Throwable)
}

sealed class Change {
   object Load : Change() {
      data class Success(val data: String) : Change()
      data class Failure(val error: Throwable): Change()
   }
}

sealed class Action {
   object Load : Action()
}

val knot = knot<State, Change, Action> {
    state {
        initial = State.Empty
    }
    changes {
        reduce { change ->
            when (change) {
                is Change.Load -> State.Loading + Action.Load
                is Change.Load.Success -> State.Content(data).only
                is Change.Load.Failure -> State.Failed(error).only
            }
        }
    }
    actions {
        perform<Action.Load> {
            switchMapSingle<String> { api.load() }
                .map<Change> { Change.Load.Success(it) }
                .onErrorReturn { Change.Load.Failure(it) }
        }
    }
    events {
        source {
            dataChangeObserver.signal.map { Change.Load }
        }
    }
    watch {
        state { println("state: $it") }
        changes { println("change: $it") }
        actions { println("action: $it") }
    }
}

knot.change.accept(Change.Load)
```

Notice how inside `reduce` function a new `State` can be combined with `Action` using `+` operator. Pure `State` can be returned from the reducer by adding `.only` suffix to the `State`.

# Composition

If your knot becomes big and you want to improve its redability and maintainability you may consider to decompose it. You start decomposition by grouping related functionality into, in a certain sense, indecomposable pieces called `Primes`. 

<img src="docs/diagrams/flowchart-composite-knot.png" width="625" />

Each `Prime` is isolated from the other `Primes`. It defines its own set of `Changes`, `Actions` and `Reducers`. It's only the `State`, what is shared between the `Primes`. In that respect each `Prime` can be seen as a separate `Knot` working on a shared `State`. Once all `Primes` are defined, they can be composed together and provided to `CompositeKnot` which implements standard `Knot` interface.

# Download
```kotlin
dependencies {
    implementation 'de.halfbit:knot:<version>'
}
```

# Inspiration
Knot was inspired by two awesome projects
* Krate https://github.com/gustavkarlsson/krate
* Redux-loop https://github.com/redux-loop/redux-loop

# License
```
Copyright 2019 Sergej Shafarenka, www.halfbit.de

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
