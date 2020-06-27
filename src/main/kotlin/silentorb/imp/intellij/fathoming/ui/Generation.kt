package silentorb.imp.intellij.fathoming.ui

import silentorb.mythic.fathom.misc.DistanceFunction
import silentorb.mythic.fathom.misc.ShadingFunction
import silentorb.mythic.fathom.surfacing.Edges
import silentorb.mythic.fathom.surfacing.VertexFace
import silentorb.mythic.fathom.surfacing.getSceneGridBounds
import silentorb.mythic.fathom.surfacing.old.*
import silentorb.mythic.fathom.surfacing.old.marching.marchingCubes
import silentorb.mythic.fathom.surfacing.vertexList
import silentorb.mythic.lookinglass.serializeNormal
import silentorb.mythic.lookinglass.serializeShadingColor
import silentorb.mythic.spatial.Vector3
import silentorb.mythic.spatial.getNormal
import silentorb.mythic.spatial.toList
import silentorb.mythic.spatial.toVector3

fun simplify(vertices: FloatArray): FloatArray {
  val fullVectorList = (vertices.indices step 3)
      .map {
        Vector3(vertices[it], vertices[it + 1], vertices[it + 2])
      }

  val minimalVectorList = fullVectorList
      .distinct()

  val vertexMap = minimalVectorList
      .mapIndexed { index, vector3 -> Pair(vector3, index) }
      .associate { it }

  val inputMesh = Mesh(
      vertices = vertexMap.keys.map { position ->
        Vertex(
            position = position
        )
      },
      triangles = (fullVectorList.indices step 3)
          .map { i ->
            Triangle(
                indices = (0 until 3).map { offset ->
                  vertexMap[fullVectorList[i + offset]]!!
                }.toIntArray()
            )
          }
          .toTypedArray()
  )

  val outputMesh = simplifyMesh(inputMesh.triangles.size / 2, 7f, inputMesh)

  println("Reduce ${inputMesh.triangles.size} to ${outputMesh.triangles.size}")
  return outputMesh.triangles
      .flatMap { triangle ->
        val normal = triangle.normal
        triangle.indices.flatMap { index ->
          val position = inputMesh.vertices[index].position
          listOf(position.x, position.y, position.z)
              .plus(listOf(-normal.x, -normal.y, -normal.z))
        }
      }
      .toFloatArray()
}

fun generateShadedMesh(getDistance: DistanceFunction, getShading: ShadingFunction): FloatArray {
  val voxelsPerUnit = 10
  val bounds = getSceneGridBounds(getDistance, 1f)
      .pad(1)
  val dimensions = bounds.end - bounds.start
  val voxelDimensions = dimensions * voxelsPerUnit
  val voxels = voxelize(getDistance, voxelDimensions, 1, 1f / voxelsPerUnit.toFloat())
  return marchingCubes(voxels, voxelDimensions, (dimensions - dimensions / 2).toVector3(), 0.5f) { vertexBuffer, a, b, c ->
    val normal = getNormal(a, b, c)
    val shadingA = getShading(a)
    val shadingB = getShading(b)
    val shadingC = getShading(c)
    val serializedNormal = serializeNormal(normal)
    vertexBuffer.addAll(listOf(a.x, a.y, a.z) + serializeShadingColor(shadingA) + serializedNormal)
    vertexBuffer.addAll(listOf(b.x, b.y, b.z) + serializeShadingColor(shadingB) + serializedNormal)
    vertexBuffer.addAll(listOf(c.x, c.y, c.z) + serializeShadingColor(shadingC) + serializedNormal)
  }
}

fun generateShadedMesh(source: MeshSource): FloatArray {
  return source.faces
      .flatMap { points ->
        val normal = (points[1] - points[0]).normalize().cross((points[2] - points[1]).normalize())
        val first = points.first()
        (1 until points.size - 1)
            .flatMap { i ->
              listOf(
                  first,
                  normal,
                  points[i],
                  normal,
                  points[i + 1],
                  normal
              )
                  .flatMap(::toList)
            }
      }
      .toFloatArray()
}

data class MeshSource(
    val edges: Edges,
    val faces: List<VertexFace>
)

fun generateWireframeMesh(mesh: MeshSource): FloatArray {
  return mesh.edges
      .flatMap(::vertexList)
      .flatMap(::toList)
      .toFloatArray()
}
