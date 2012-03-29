package workers

import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.util.duration._

object Scheduler {

  lazy val scheduledTask =
    Akka.system.scheduler.schedule(1 second, 1 minute) { job }

  def job() {
    import models._
    import controllers.Helpers._
    import org.squeryl.PrimitiveTypeMode._
    Logger.info("Running scheduled job")

    val now: java.sql.Timestamp = timeNow

    val urls = transaction {
      from(Storage.schedule) { task =>
        where(task.deletionTime <= now)
        select(task.url)
      }
    }

    val deletedUrls = transaction {
      urls filter { url =>
        val dir = Storage.root / url
        val (deleted, remains) = dir.deleteRecursively(continueOnFailure = true)
        val msg = "Deleting " + dir + " [" + deleted + "," + remains + "]"
        if (deleted == 2) Logger.debug(msg) else Logger.error(msg)
        remains == 0
      }
    }

    transaction(Storage.files deleteWhere { _.url in deletedUrls })
    transaction(Storage.schedule deleteWhere { _.url in deletedUrls })

  }

  def start() {
    Logger.info("Scheduler started")
    scheduledTask
  }

  def stop() {
    Logger.info("Scheduler stopped")
    scheduledTask.cancel()
  }
}
