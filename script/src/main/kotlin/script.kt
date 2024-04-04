package cnedclub.sad.script

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
    defaultImports("cnedclub.sad.*")
    defaultImports("cnedclub.sad.canvas.*")
    defaultImports("cnedclub.sad.canvas.BlendEquation.*")
    defaultImports("cnedclub.sad.canvas.BlendFunc.*")
    defaultImports("cnedclub.sad.canvas.GradientDirection.*")
    defaultImports("cnedclub.sad.script.OptimizationType.*")
    defaultImports("cnedclub.sad.canvas.TransformOp.OutOfBoundsFill")

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
