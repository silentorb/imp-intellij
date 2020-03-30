package silentorb.imp.intellij.ui.substance

import silentorb.imp.core.Graph
import silentorb.imp.core.Id
import silentorb.imp.core.getGraphOutputNode
import silentorb.imp.execution.FunctionImplementationMap
import silentorb.imp.execution.execute
import silentorb.mythic.imaging.substance.Sampler3dFloat
import silentorb.mythic.imaging.substance.marching.marchingCubes
import silentorb.mythic.imaging.substance.voxelize
import silentorb.mythic.spatial.Vector3i
import silentorb.mythic.spatial.toVector3

fun generateMesh(functions: FunctionImplementationMap, graph: Graph, node: Id?): FloatArray? {
    val output = node ?: getGraphOutputNode(graph)
    val values = execute(functions, graph)
    val value = values[output]
    return if (value == null)
        null
    else {
        val voxelsPerUnit = 20
        val unitDimensions = Vector3i(5)
        val voxelDimensions = unitDimensions * voxelsPerUnit
        val voxels = voxelize(value as Sampler3dFloat, voxelDimensions, 1, 1f / voxelsPerUnit.toFloat())
        marchingCubes(voxels, voxelDimensions, (unitDimensions - unitDimensions / 2).toVector3(), 0.5f)
    }
}
