package silentorb.imp.intellij.fathoming.ui

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import silentorb.imp.intellij.fathoming.state.DisplayMode
import silentorb.imp.intellij.fathoming.state.getDisplayMode
import silentorb.mythic.desktop.createHeadlessWindow
import silentorb.mythic.desktop.initializeDesktopPlatform
import silentorb.mythic.glowing.*
import silentorb.mythic.imaging.texturing.Bitmap
import silentorb.mythic.imaging.texturing.bitmapToBufferedImage
import silentorb.mythic.lookinglass.*
import silentorb.mythic.lookinglass.meshes.Primitive
import silentorb.mythic.lookinglass.meshes.VertexSchemas
import silentorb.mythic.lookinglass.shading.ObjectShaderConfig
import silentorb.mythic.lookinglass.shading.ShaderFeatureConfig
import silentorb.mythic.platforming.WindowInfo
import silentorb.mythic.scenery.*
import silentorb.mythic.spatial.*
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.tan

var hiddenWindow: Long? = null

fun newRenderer(): Renderer {
  initializeDesktopPlatform()
  hiddenWindow = createHeadlessWindow()

  return emptyRenderer(
      config = DisplayConfig(

      )
  )
      .copy(
          offscreenBuffers = listOf(
              prepareScreenFrameBuffer(1024, 1024, true)
          )
      )
}

const val dynamicMeshId = "dynamic"

fun newMesh(vertices: FloatArray, vertexSchemas: VertexSchemas): ModelMesh {
  val displayMode = getDisplayMode()
  return ModelMesh(
      id = dynamicMeshId,
      primitives = listOf(
          Primitive(
              mesh = if (displayMode == DisplayMode.wireframe)
                GeneralMesh(
                    vertexSchema = vertexSchemas.flat,
                    vertexBuffer = newVertexBuffer(vertexSchemas.flat).load(createFloatBuffer(vertices)),
                    count = vertices.size,
                    primitiveType = PrimitiveType.lineSegments
                )
              else
                GeneralMesh(
                    vertexSchema = vertexSchemas.shaded,
                    vertexBuffer = newVertexBuffer(vertexSchemas.shaded).load(createFloatBuffer(vertices)),
                    count = vertices.size,
                    primitiveType = PrimitiveType.triangles
                ),
              material = Material(
                  color = Vector4(1f, 0f, 0f, 1f),
                  shading = displayMode != DisplayMode.wireframe
              )
          )
      )
  )
}

private var staticRenderer: Renderer? = null
fun rendererSingleton(): Renderer {
  if (staticRenderer == null) {
    staticRenderer = newRenderer()
  }
  return staticRenderer!!
}

fun createScene(cameraState: CameraState) =
    Scene(
        camera = Camera(
            ProjectionType.perspective,
            Vector3(-cameraState.distance, 0f, 0f)
                .transform(Matrix.identity
                    .rotateZ(cameraState.yaw)
                    .rotateY(cameraState.pitch)
                ),
            Quaternion()
                .rotateZ(cameraState.yaw)
                .rotateY(cameraState.pitch),
            45f
        ),
        lights = listOf(
            Light(
                type = LightType.point,
                color = Vector4(1f),
                offset = Vector3(0f, 5f, 10f),
                range = 20f
            )
        ),
        lightingConfig = LightingConfig(ambient = 0.3f)
    )

fun getNearPlaneHeight(viewport: Vector4i, fov: Float): Float =
    abs(viewport.w - viewport.y).toFloat() / (2f * tan(0.5f * radiansToDegrees(fov)))

fun renderMesh(vertices: FloatArray, dimensions: Vector2i, cameraState: CameraState): BufferedImage {
  val scene = createScene(cameraState)
  val initialRenderer = rendererSingleton()
  val vertexSchema = initialRenderer.vertexSchemas.shadedPoint
  val mesh = GeneralMesh(
      vertexSchema = vertexSchema,
      vertexBuffer = newVertexBuffer(vertexSchema).load(createFloatBuffer(vertices)),
      count = vertices.size / vertexSchema.floatSize,
      primitiveType = PrimitiveType.points
  )
  println(vertices.size / 3)
  try {
    val renderer = initialRenderer
    val windowInfo = WindowInfo(dimensions = dimensions)
    renderContainer(renderer, windowInfo) {
      val glow = renderer.glow
      glow.state.vertexProgramPointSizeEnabled = true
      glow.state.clearColor = Vector4(1f, 1f, 0f, 1f)
      val offscreenBuffer = renderer.offscreenBuffers.first()
      val viewport = Vector4i(0, 0, dimensions.x, dimensions.y)
      val sceneRenderer = createSceneRenderer(renderer, scene, viewport)
      glow.state.setFrameBuffer(offscreenBuffer.framebuffer.id)
      glow.state.viewport = viewport
      glow.operations.clearScreen()
//      glow.state.pointSize = 7f
//      val material = Material(
//          color = Vector4(1f, 0f, 0f, 1f),
//          shading = true
//      )
      val effect = renderer.getShader(vertexSchema, ShaderFeatureConfig(
          shading = true,
          pointSize = true
      ))

      val config = ObjectShaderConfig(
//          color = Vector4(1f, 0f, 0f, 1f),
          nearPlaneHeight = getNearPlaneHeight(viewport, scene.camera.angleOrZoom)
      )

      effect.activate(config)
      drawMesh(mesh, GL11.GL_POINTS)

      checkError("It worked")
    }
  } finally {
    mesh.vertexBuffer.dispose()
  }
  val buffer = BufferUtils.createFloatBuffer(dimensions.x * dimensions.y * 3)
  GL11.glReadPixels(0, 0, dimensions.x, dimensions.y, GL11.GL_RGB, GL11.GL_FLOAT, buffer)
  return bitmapToBufferedImage(Bitmap(buffer = buffer, channels = 3, dimensions = dimensions))
}
