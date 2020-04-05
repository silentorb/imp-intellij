package silentorb.imp.intellij.fathoming.ui

import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.execution.execute
import silentorb.mythic.imaging.fathoming.DistanceFunction
import silentorb.mythic.imaging.fathoming.surfacing.*
import silentorb.mythic.imaging.fathoming.surfacing.old.*
import silentorb.mythic.imaging.fathoming.surfacing.old.marching.marchingCubes
import silentorb.mythic.spatial.Vector3
import silentorb.mythic.spatial.Vector3i
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

fun executeGraph(functions: FunctionImplementationMap, graph: Graph, node: Id?): Any? {
    val output = node ?: getGraphOutputNode(graph)
    val values = execute(functions, graph)
    return values[output]
}

fun generateShadedMesh(value: DistanceFunction): FloatArray {
    val voxelsPerUnit = 10
    val unitDimensions = Vector3i(5)
    val voxelDimensions = unitDimensions * voxelsPerUnit
    val voxels = voxelize(value, voxelDimensions, 1, 1f / voxelsPerUnit.toFloat())
    return marchingCubes(voxels, voxelDimensions, (unitDimensions - unitDimensions / 2).toVector3(), 0.5f)
}

fun generateWireframeMesh(getDistance: DistanceFunction): FloatArray {
    val config = SurfacingConfig(
        getDistance = getDistance,
        tolerance = 0.01f,
        cellSize = 1f,
        subCells = 32
    )

    val bounds = getSceneGridBounds(getDistance, config.cellSize)
//        .pad(1)

    val traceCell = traceCellEdges(config, bounds)
    val edges = traceAll(bounds, traceCell)
    return edges
        .flatMap(::vertexList)
        .flatMap(::toList)
        .toFloatArray()
}
