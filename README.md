# Knot

[![Build Status](https://travis-ci.org/beworker/knot.svg?branch=master)](https://travis-ci.org/beworker/knot)
[![Maven Central](http://img.shields.io/maven-central/v/de.halfbit/knot.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.halfbit%22%20a%3A%22knot%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Consice reactive state container library for Android applications.

# Concept

Knot helps managing application state by reacting on events and performing asynchronous actions in a structured way. There are five core concepts Knot defines: `State`, `Change`, `Reducer`, `Effect` and `Action`.

<img src="docs/diagrams/flowchart-knot.png" width="490" />

`State` represents an immutable partial state of an Android application. It can be a state of a screen or a state of an internal headless component, like repository.

`Change` is an immutable data object with an optional payload intended for changing the `State`. A `Change` can be produced from an external event or be a result of execution of an `Action`.

`Action` is a synchronous or an asynchronous operation which, when completed, can emit a new `Change`.

`Reducer` is a function that takes the previous `State` and a `Change` as arguments and returns the new `State` and an optional `Action` wrapped by `Effect` class.

`Effect` is a convenient wrapper class containing the new `State` and an optional `Action`. If `Action` is present, Knot will perform it and provide resulting `Change` back to `Reducer`.

# Composition

If your knot becomes big and you want to improve its maintainability and extensibility you may consider to decompose it. You start decomposition by grouping related functionality into, in a certain sense, indecomposable pieces called `Prime`'s. 

<img src="docs/diagrams/flowchart-composite-knot.png" width="625" />

`Prime` defines its own `Change`'s, `Action`'s and reducer for own changes. It's only the `State`, what is shared between the `Prime`'s. In that respect each `Prime` can be considered to be a separate knot working on a shared `State`. Once all `Prime`'s are defined, they can be composed together and provided to `CompositeKnot` which implements standard knot interface. 

# Why Knot?

* Structured - helps writing better structured and less buggy code.
* Modular - single knots can be combined together to build more complex application logic.
* Decomposable - complex knots can be decomposed into primes by related functionality.
* Consice - it has minimalistic API and compact implementation.
* Consistent - state is the single source of truth.
* Testable - reducers and transformers are easy to test. 
* DSL - easy to read and write declarative configuration language.
* Why not?

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
