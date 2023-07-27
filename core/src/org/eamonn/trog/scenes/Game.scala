package org.eamonn.trog
package scenes

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import org.eamonn.trog.Scene
import org.eamonn.trog.procgen.{GeneratedMap, Level}

import scala.util.Random

class Game(lvl: Level, plr: Player) extends Scene {
  var keysDown: List[Int] = List.empty
  var level: Level = lvl
  var descending = false
  var player: Player = plr
  var enemyTurn = false
  var cameraLocation: Vec2 = Vec2(0, 0)
  var updatingCameraX = false
  var updatingCameraY = false
  var allSpawned = false
  var clicked = false
  var mouseLocOnGrid: Vec2 = Vec2(0, 0)
  var enemies: List[Enemy] = List.empty
  override def init(): InputAdapter = {
    player.game = this
    new GameControl(this)
  }

  def updateCamera(): Unit = {
    if (updatingCameraX) {
      if (
        cameraLocation.x < (Geometry.ScreenWidth / 2 / screenUnit).toInt - player.location.x
      ) cameraLocation.x += 1
      else if (
        cameraLocation.x > (Geometry.ScreenWidth / 2 / screenUnit).toInt - player.location.x
      ) cameraLocation.x -= 1
      else updatingCameraX = false
    }
    if (updatingCameraY) {
      if (
        cameraLocation.y < (Geometry.ScreenHeight / 2 / screenUnit).toInt - player.location.y
      ) cameraLocation.y += 1
      else if (
        cameraLocation.y > (Geometry.ScreenHeight / 2 / screenUnit).toInt - player.location.y
      ) cameraLocation.y -= 1
      else updatingCameraY = false
    }
  }
  override def update(delta: Float): Option[Scene] = {
    if (!allSpawned) {
      for (i <- 0 until 10) {
        var loc = level.walkables.filterNot(w =>
          player.location == w && enemies.exists(e => e.location == w)
        )(
          Random.nextInt(
            level.walkables
              .filterNot(w =>
                player.location == w && enemies.exists(e => e.location == w)
              )
              .length
          )
        )
        var enemy = IceImp()
        enemy.location = loc
        enemy.game = this
        enemy.health = enemy.maxHealth
        enemies = enemy :: enemies
      }
      allSpawned = true
    }
    if (
      player.location.x < -cameraLocation.x + 5 || player.location.x > -cameraLocation.x + (Geometry.ScreenWidth / screenUnit) - 5
    ) updatingCameraX = true
    if (
      player.location.y < -cameraLocation.y + 5 || player.location.y > -cameraLocation.y + (Geometry.ScreenHeight / screenUnit) - 5
    ) updatingCameraY = true
    if (updatingCameraX || updatingCameraY) updateCamera()
    player.update(delta)
    enemies.foreach(e => e.update(delta))
    enemyTurn = false
    if (descending) Some(new LevelGen(player, Some(this)))
    else
      None
  }

  override def render(batch: PolygonSpriteBatch): Unit = {
    Trog.translationX = cameraLocation.x
    Trog.translationY = cameraLocation.y
    level.draw(batch)
    player.draw(batch)
    enemies.foreach(e => e.draw(batch))
    for (
      x <-
        -cameraLocation.x - 5 to (-cameraLocation.x + Geometry.ScreenWidth / screenUnit).toInt + 5
    ) {
      for (
        y <-
          -cameraLocation.y - 5 to (-cameraLocation.y + Geometry.ScreenHeight / screenUnit).toInt + 5
      ) {
        var dist = Int.MaxValue

        if (
          Math
            .sqrt(
              ((x - player.location.x) * (x - player.location.x)) + ((y - player.location.y) * (y - player.location.y))
            )
            .toInt < player.sightRad
        ) {
          var path =
            Pathfinding.findPathUpto(player.location, Vec2(x, y), level)
          if (path.nonEmpty) {
            path.foreach(p => {
              dist = p.list.length
            })
          }
        }
        if (dist > player.sightRad) {
          batch.setColor(0, 0, 0, 1)
        } else {
          var lightLevel: Float =
            ((((player.sightRad - dist).toFloat / player.sightRad) + .25f) min 1) max 0
          batch.setColor(
            0,
            0,
            0,
            1 - lightLevel
          )
        }
        batch.draw(
          Trog.Square,
          x * screenUnit,
          y * screenUnit,
          screenUnit,
          screenUnit
        )
      }
    }
  }

  def renderUI(batch: PolygonSpriteBatch): Unit = {
    batch.setColor(Color.WHITE)
    Text.smallFont.draw(
      batch,
      s"Lvl ${player.level}",
      -cameraLocation.x * screenUnit,
      -cameraLocation.y * screenUnit + Geometry.ScreenHeight
    )
    batch.setColor(Color.YELLOW)
    batch.draw(
      Trog.Square,
      -cameraLocation.x * screenUnit,
      -cameraLocation.y * screenUnit + Geometry.ScreenHeight - (screenUnit),
      screenUnit * 4,
      screenUnit / 8
    )
    batch.setColor(Color.ORANGE)
    batch.draw(
      Trog.Square,
      -cameraLocation.x * screenUnit,
      -cameraLocation.y * screenUnit + Geometry.ScreenHeight - (screenUnit),
      screenUnit * 4 * player.exp / player.nextExp,
      screenUnit / 8
    )
    batch.setColor(Color.FIREBRICK)
    batch.draw(
      Trog.Square,
      -cameraLocation.x * screenUnit,
      -cameraLocation.y * screenUnit + Geometry.ScreenHeight - (screenUnit * 3 / 2),
      screenUnit * 4,
      screenUnit / 2
    )
    batch.setColor(Color.RED)
    batch.draw(
      Trog.Square,
      -cameraLocation.x * screenUnit,
      -cameraLocation.y * screenUnit + Geometry.ScreenHeight - (screenUnit * 3 / 2),
      screenUnit * 4 * player.health / player.maxHealth,
      screenUnit / 2
    )
    batch.setColor(Color.WHITE)
    Text.smallFont.draw(
      batch,
      s"${player.health}/${player.maxHealth}",
      -cameraLocation.x * screenUnit,
      -cameraLocation.y * screenUnit + Geometry.ScreenHeight - screenUnit
    )
  }
}
class GameControl(game: Game) extends InputAdapter {
  override def keyDown(keycode: Int): Boolean = {
    game.keysDown = keycode :: game.keysDown
    true
  }
  override def keyUp(keycode: Int): Boolean = {
    game.keysDown = game.keysDown.filterNot(f => f == keycode)
    true
  }

  override def mouseMoved(screenX: Int, screenY: Int): Boolean = {
    game.mouseLocOnGrid.x =
      (screenX / screenUnit).floor.toInt - game.cameraLocation.x
    game.mouseLocOnGrid.y =
      ((Geometry.ScreenHeight - screenY) / screenUnit).floor.toInt - game.cameraLocation.y
    true
  }

  override def touchDown(
      screenX: Int,
      screenY: Int,
      pointer: Int,
      button: Int
  ): Boolean = {
    game.clicked = true
    true
  }

  override def touchUp(
      screenX: Int,
      screenY: Int,
      pointer: Int,
      button: Int
  ): Boolean = {
    game.clicked = false
    true
  }
}
