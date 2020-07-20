package silentorb.imp.intellij.fathoming.ui

import silentorb.mythic.fathom.marching.marchingMesh
import silentorb.mythic.fathom.misc.DistanceFunction
import silentorb.mythic.fathom.misc.ShadingFunction
import silentorb.mythic.lookinglass.IndexedGeometry
import silentorb.mythic.lookinglass.serializeVertex

fun generateShadedMesh(getDistance: DistanceFunction, getShading: ShadingFunction): IndexedGeometry {
  val (vertices, triangles) = marchingMesh(10, getDistance, getShading)
  val vertexFloats = vertices
      .flatMap(::serializeVertex)
      .toFloatArray()

  return IndexedGeometry(vertexFloats, triangles)
}
