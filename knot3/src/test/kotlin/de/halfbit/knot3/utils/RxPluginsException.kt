package de.halfbit.knot3.utils

import io.reactivex.rxjava3.plugins.RxJavaPlugins
import org.hamcrest.core.IsEqual
import org.junit.Assert
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.reflect.KClass

class RxPluginsException private constructor() : TestRule {

    private var expectedType: KClass<*>? = null
    private var expectedInstance: Throwable? = null

    fun expect(type: KClass<*>) {
        expectedType = type
    }

    fun expect(instance: Throwable) {
        expectedInstance = instance
    }

    override fun apply(base: Statement, description: Description): Statement {
        return ExpectedExceptionStatement(base)
    }

    private inner class ExpectedExceptionStatement(
        private val base: Statement
    ) : Statement() {
        override fun evaluate() {
            var observedException: Throwable? = null
            val backup = RxJavaPlugins.getErrorHandler()
            RxJavaPlugins.setErrorHandler {
                observedException = it
            }
            try {
                base.evaluate()
            } finally {
                RxJavaPlugins.setErrorHandler(backup)
            }
            when {
                expectedInstance != null -> {
                    Assert.assertThat(observedException, IsEqual(expectedInstance))
                }
                expectedType != null -> {
                    val observedType = observedException?.let { it::class }
                    Assert.assertThat(observedType, IsEqual(expectedType))
                }
            }
        }
    }

    companion object {
        fun none() = RxPluginsException()
    }
}