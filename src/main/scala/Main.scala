package fr.game.pkg

import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.{Game, Gdx, Input}
import com.badlogic.gdx.graphics.{Color, GL20, PerspectiveCamera}
import com.badlogic.gdx.graphics.g3d.attributes.{BlendingAttribute, ColorAttribute}
import com.badlogic.gdx.graphics.g3d._
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.{CameraInputController, ModelBuilder}
import com.badlogic.gdx.math.Vector3

import scala.collection.mutable.ArrayBuffer

class MyGame extends Game {

  var modelBatch: ModelBatch = null
  var cam: PerspectiveCamera = null
  var camControl: CameraInputController = null
  var airEnvir: Environment = null
  var waterEnvir: Environment = null

  var parts = ArrayBuffer.empty[CubePart]

  var physics: Physics = null

  var waterModel: Model = null
  var waterSurface: Array[ModelInstance] = null

  def createDumbPart = {
    val size = Math.random().toFloat * 10f
    val col = new Color(Math.random().toFloat, Math.random().toFloat, Math.random().toFloat, 1f)
    val part = new CubePart(physics, size, col)
    val range = 40f
    part.move(Math.random().toFloat * range * 2f - range, Math.random().toFloat * range * 2f - range, Math.random().toFloat * range)
    part
  }

  override def create() = {
    physics = new Physics
    modelBatch = new ModelBatch
    cam = createCam

    val mainPart = new MainPart(physics, 5f, Color.WHITE)
    mainPart.move(0f, 0f, 20f)
    parts.append(mainPart)

    for (i <- 0 to 30)
      parts.append(createDumbPart)

    val builder = new ModelBuilder
    waterModel = builder.createRect (
      -1f, -1f, 0f,
      1f, -1f, 0f,
      1f, 1f, 0f,
      -1f, 1f, 0f,
      0f, 0f, 1f,
      new Material(
        ColorAttribute.createDiffuse(Color.BLUE),
        new BlendingAttribute(true, 0.6f)),
      Usage.Position | Usage.Normal)
    waterSurface = Array(new ModelInstance(waterModel), new ModelInstance(waterModel))
    waterSurface(0).transform.scale(100f, 100f, 100f)
    waterSurface(1).transform.scale(100f, 100f, 100f)
    waterSurface(1).transform.rotate(1f, 0f, 0f, 180f)

    airEnvir = new Environment
    airEnvir.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
    airEnvir.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

    waterEnvir = new Environment
    waterEnvir.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.4f, 1f))
    waterEnvir.set(new ColorAttribute(ColorAttribute.Fog, 0f, 0f, 0.13f, 1f))
    waterEnvir.add(new DirectionalLight().set(0.6f, 0.6f, 0.8f, -1f, -0.8f, -0.2f))

    camControl = new CameraInputController(cam)
    Gdx.input.setInputProcessor(camControl)
  }

  private def createCam = {
    val cam = new PerspectiveCamera(90, Gdx.graphics.getWidth, Gdx.graphics.getHeight)
    cam.position.set(20f, 20f, 20f)
    cam.lookAt(0f, 0f, 0f)
    cam.up.set(0f, 0f, 1f)
    cam.near = 1f
    cam.far = 300f
    cam.update()
    cam
  }

  override def render() = {

    val delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime)
    physics.step(delta)
    parts.foreach(_.update())

    moveCamera(delta)

    renderThings

    if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
      Gdx.app.exit()
    }

  }

  def renderThings: Unit = {
    val envir = if (cam.position.z > 0f) airEnvir else waterEnvir
    if (cam.position.z > 0f)
      Gdx.gl.glClearColor(0f, 0f, 0f, 1)
    else
      Gdx.gl.glClearColor(0f, 0f, 0.13f, 1)

    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth, Gdx.graphics.getHeight)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)
    modelBatch.begin(cam)
    parts.foreach(p => modelBatch.render(p.instance, envir))
    waterSurface.foreach(instance => modelBatch.render(instance, envir))
    modelBatch.end()
  }

  def moveCamera(delta: Float): Unit = {
    camControl.update()
    val pos = new Vector3
    parts(0).instance.transform.getTranslation(pos)
    cam.lookAt(pos)
    val camToPatou = pos.cpy().sub(cam.position)
    val maxCameraDistance = 40f
    if (camToPatou.len() > maxCameraDistance) {
      val l = camToPatou.len()
      val catchup = camToPatou.scl((1 - Math.pow(0.05f, delta)).toFloat * (l - maxCameraDistance) / l)
      cam.translate(catchup)
    }
    cam.update()
  }

  override def dispose() = {
    modelBatch.dispose()
    parts.foreach(_.dispose())
  }

}

object Main extends App {
  val cfg = new LwjglApplicationConfiguration
  cfg.title = "Water Patou"
  cfg.height = 480
  cfg.width = 800
  cfg.forceExit = false
  new LwjglApplication(new MyGame, cfg)
}
