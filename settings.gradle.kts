import java.nio.file.Files
import java.nio.file.Path

//fun includeProject(path: Path) {
//  println(path.fileName)
//  include(path.fileName.toString())
//  project(":${path.fileName}").projectDir = path.toFile()
//}

//Files.list(file("../imp/projects").toPath())
//    .forEach(::includeProject)

includeBuild("../imp")

includeBuild("../mythic")
