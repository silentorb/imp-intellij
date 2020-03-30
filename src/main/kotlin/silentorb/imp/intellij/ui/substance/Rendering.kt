package silentorb.imp.intellij.ui.substance

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import silentorb.mythic.desktop.createHeadlessWindow
import silentorb.mythic.desktop.initializeDesktopPlatform
import silentorb.mythic.glowing.*
import silentorb.mythic.imaging.texturing.Bitmap
import silentorb.mythic.imaging.texturing.bitmapToBufferedImage
import silentorb.mythic.lookinglass.*
import silentorb.mythic.lookinglass.meshes.Primitive
import silentorb.mythic.platforming.WindowInfo
import silentorb.mythic.scenery.*
import silentorb.mythic.spatial.*
import java.awt.image.BufferedImage

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

fun newMesh(vertices: FloatArray, vertexSchema: VertexSchema): ModelMesh {
  return ModelMesh(
      id = dynamicMeshId,
      primitives = listOf(
          Primitive(
              mesh = GeneralMesh(
                  vertexSchema = vertexSchema,
                  vertexBuffer = newVertexBuffer(vertexSchema).load(createFloatBuffer(vertices)),
                  count = vertices.size / 6
              ),
              material = Material(
                  color = Vector4(1f, 0f, 0f, 1f),
                  shading = true
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

fun createScene(cameraState: CameraState): GameScene {
  return GameScene(
      main = Scene(
          camera = Camera(
              ProjectionType.perspective,
              Vector3(-cameraState.distance, 0f, 0f).transform(Matrix.identity.rotateZ(cameraState.yaw)),
              Quaternion().rotateZ(cameraState.yaw),
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
          lightingConfig = LightingConfig(ambient = 0.2f)
      ),
      opaqueElementGroups = listOf(
          ElementGroup(
              meshes = listOf(
                  MeshElement(
                      id = 1L,
                      mesh = dynamicMeshId,
                      transform = Matrix.identity
                  )
              )
          )
      ),
      transparentElementGroups = listOf(),
      filters = listOf(),
      background = listOf()
  )
}

fun renderMesh(vertices: FloatArray, dimensions: Vector2i, cameraState: CameraState): BufferedImage {
  val scene = createScene(cameraState)
  val initialRenderer = rendererSingleton()
  val mesh = newMesh(vertices, initialRenderer.vertexSchemas.shaded)
  try {
    val renderer = initialRenderer
        .copy(
            meshes = mapOf(
                dynamicMeshId to mesh
            )
        )
    val windowInfo = WindowInfo(dimensions = dimensions)
    renderContainer(renderer, windowInfo) {
      val glow = renderer.glow
      glow.state.clearColor = Vector4(1f, 1f, 0f, 1f)
      val offscreenBuffer = renderer.offscreenBuffers.first()
      val viewport = Vector4i(0, 0, dimensions.x, dimensions.y)
      val sceneRenderer = createSceneRenderer(renderer, scene, viewport)
      glow.state.setFrameBuffer(offscreenBuffer.framebuffer.id)
      glow.state.viewport = Vector4i(0, 0, dimensions.x, dimensions.y)
      glow.operations.clearScreen()
      glow.state.cullFaces = false
      renderElements(sceneRenderer, scene.opaqueElementGroups, scene.transparentElementGroups)
      checkError("It worked")
    }
  } finally {
    mesh.primitives.first().mesh.vertexBuffer.dispose()
  }
  val buffer = BufferUtils.createFloatBuffer(dimensions.x * dimensions.y * 3)
  GL11.glReadPixels(0, 0, dimensions.x, dimensions.y, GL11.GL_RGB, GL11.GL_FLOAT, buffer)
  return bitmapToBufferedImage(Bitmap(buffer = buffer, channels = 3, dimensions = dimensions))
}
