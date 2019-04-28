# Knot

[![Maven Central](http://img.shields.io/maven-central/v/de.halfbit/knot.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.halfbit%22%20a%3A%22knot%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Lightweight reactive state container library for Android applications.

# Concept

Knot helps managing application state, external events and asynchronous actions in a structured way. There are five core concepts Knot defines: `State`, `Change`, `Reducer`, `Effect` and `Action`.

<img src="docs/diagrams/flowchart.png" width="500" />

`State` represents an immutable partial state of an Android application. It can be a state of a screen or a state of an internal headless component, like repository.

`Change` is an immutable data object with an optional payload intended for changing the `State`. A `Change` can be produced from an external event or be a result of execution of an `Action`.

`Action` is an asynchronous operation which, when completed, can emit a new `Change`.

`Reducer` is a function that takes the previous `State` and a `Change` as arguments and returns an `Effect`. 

`Effect` is a convenient wrapper class containing a new `State` and an optional `Action` which, when it's present in `Effect`, will be performed.

# Why Knot?

* Simple - main Knot class has about 60 lines of code.
* Predictable - helps writing better structured and less buggy code.
* Modular - single knots can be combined together to build more complex application logic.
* Testable - reducer function is easy to test. 
* DSL - easy to read declarative configuration.

# Considerations

* In contrast to the most of state container implementations out there, `Action` in Knot is an executable object and not a data class. This is done to reduce boilerplate code needed for defining actions and writing an action creator class. This decision can be revisit in the future however.

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
