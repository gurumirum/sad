package gurumirum.sad.script

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    fileExtension = "sad.kts",
    compilationConfiguration = ConfigScriptCompilationConfig::class
)
abstract class ConfigScript

object ConfigScriptCompilationConfig : ScriptCompilationConfiguration({
    defaultImports("gurumirum.sad.*")
    defaultImports("gurumirum.sad.canvas.*")
    defaultImports("gurumirum.sad.canvas.BlendEquation.*")
    defaultImports("gurumirum.sad.canvas.BlendFunc.*")
    defaultImports("gurumirum.sad.canvas.GradientDirection.*")
    defaultImports("gurumirum.sad.script.OptimizationType.*")
    defaultImports("gurumirum.sad.canvas.TransformOp.OutOfBoundsFill")

    implicitReceivers(ConfigScriptRoot::class)

    jvm {
        // Extract the whole classpath from context classloader and use it as dependencies
        dependenciesFromCurrentContext(wholeClasspath = true)
        compilerOptions.append("-jvm-target", "17")
    }
}) {
    private fun readResolve(): Any = ConfigScriptCompilationConfig
}

class ConfigScriptEvalConfig(root: ConfigScriptRoot) : ScriptEvaluationConfiguration({
    implicitReceivers.put(listOf(root))
})
