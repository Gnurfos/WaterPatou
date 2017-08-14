package fr.game.pkg

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.{Material, Model, ModelInstance}
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.utils.Disposable

class CubePart(physics: Physics, size: Float, color: Color) extends  Disposable {

  protected val builder = new ModelBuilder
  var model = builder.createBox(size, size, size,
    new Material(ColorAttribute.createDiffuse(color)),
    Usage.Position | Usage.Normal)
  var instance = new ModelInstance(model)
  var physicsShape = new btBoxShape(new Vector3(size / 2f, size / 2f, size / 2f))
  val volume = size * size * size
  val mass = volume * density
  var inertia = calculateInertia
  val ci = new btRigidBody.btRigidBodyConstructionInfo(mass, null, physicsShape, inertia)
  val body = new btRigidBody(ci)
  body.setCollisionShape(physicsShape)
  body.setDamping(0.2f, 0.4f)
  physics.add(body)
  sync3dToPhysics

  protected def calculateInertia = {
    val i = new Vector3
    physicsShape.calculateLocalInertia(mass, i)
    i
  }

  private def syncPhysicsTo3d = {
    body.getWorldTransform(instance.transform)
  }

  private def sync3dToPhysics = {
    body.setWorldTransform(instance.transform)
  }

  def move(x: Float, y: Float, z: Float) = {
    instance.transform.translate(x, y, z)
    sync3dToPhysics
  }

  def density = physics.woodDensity

  def update() = {
    syncPhysicsTo3d
  }

  override def dispose() = {
    model.dispose()
  }

}
