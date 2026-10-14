import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Распаковывает нативные библиотеки libGDX из jar'ов вида `*-natives-<abi>.jar`
 * в каталог `<outputDir>/<abi>` для упаковки в APK.
 */
abstract class ExtractGdxNatives : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val nativeJars: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val archives: ArchiveOperations

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun extract() {
        val abiPattern = Regex("natives-(.+)\\.jar$")
        fileSystem.sync {
            into(outputDir)
            nativeJars.files.forEach { jar ->
                val abi = abiPattern.find(jar.name)?.groupValues?.get(1)
                    ?: error("Не удалось определить ABI по имени файла: ${jar.name}")
                from(archives.zipTree(jar)) {
                    include("*.so")
                    into(abi)
                }
            }
        }
    }
}
