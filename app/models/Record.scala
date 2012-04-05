package models

import org.squeryl.KeyedEntity
import java.sql.Timestamp
import akka.util.duration._

import lib.Helpers._

class Record(val url: String,
             val name: String,
             val size: Long,
             val creationTime: Timestamp,
             val deletionTime: Timestamp,
             val password: Option[Array[Byte]],
             val question: Option[String],
             val answer: Option[Array[Byte]])
    extends KeyedEntity[String] {

  def this() = this(
    "url",
    "name",
    0,
    0 millis,
    0 millis,
    Some(Array.empty),
    Some("question"),
    Some(Array.empty))

  def id = url

  def path = Storage.root / url / name

  def getSecret(key: String) = key match {
    case "password" => password
    case "answer" => answer
    case _ => None
  }

  def readableSize = {
    val mask = "%.1f"
    def convert(size: Double, px: Seq[String]): String = {
      val next = size / 1.kB
      if (px.nonEmpty && next > 1) convert(next, px.tail)
      else mask.format(size) + " " + px.head
    }

    convert(size, bytePrefixes)
  }
}

object Record {
  import scalaz.{ Logger => _, _ }
  import Scalaz._

  import play.api.mvc._
  import MultipartFormData.FilePart
  import play.api.libs.Files.TemporaryFile

  import akka.util.Duration

  type R = Request[MultipartFormData[TemporaryFile]]

  object File {
    def apply(implicit request: R) =
      request.body.file("file").toSuccess("file not found").liftFailNel
  }

  object URL {
    def valid(url: String) =
      if (url matches """^[^\s?&]+[^?&]*$""") url.successNel
      else "invalid url".failNel

    def available(url: String) =
      getSomeFile(url).cata(_ => "url reserved".failNel, url.successNel)

    def apply(file: ValidationNEL[String, FilePart[_]])(implicit request: R) = {
      val url = (multipartParam("url"), file) match {
        case (Success(u), _) => Success(u)
        case (_, Success(f)) => Success(f.filename)
        case (_, Failure(e)) => e.fail
      }

      url flatMap valid flatMap available
    }
  }

  object Question {
    def apply(implicit request: R) = multipartParam("question")
  }

  sealed trait Secret

  case object Password extends Secret {
    def apply(time: Long)(implicit request: R) = multipartParam("password") map hash(time)
  }

  case object Answer {
    def apply(time: Long)(implicit request: R) = multipartParam("answer") map hash(time)
  }

  type V[X] = ValidationNEL[String, X]

  object Secret {
    def apply(password: V[Array[Byte]], answer: V[Array[Byte]]) =
      (password, answer) match {
        case (Failure(_), Success(a)) => Answer
        case _ => Password
      }
  }

  private[this] def prepare(file: FilePart[TemporaryFile], url: String) = {
    val FilePart(_, name, _, ref) = file
    val size = ref.file.length
    val dest = Storage.root / url / name

    /* Workaround for scala-io 'moveTo bug
       * https://github.com/jesseeichar/scala-io/issues/54*/
    scalax.file.Path(ref.file) copyTo dest
    ref.clean

    name -> size
  }

  def apply(
    file: V[FilePart[TemporaryFile]],
    url: V[String],
    password: V[Array[Byte]],
    question: V[String],
    answer: V[Array[Byte]],
    from: Duration) = {
    val to = from + lib.Config.storageTime
    Secret(password, answer) match {
      case Password => (file |@| url |@| password) { (f, u, p) =>
        val (name, size) = prepare(f, u)
        new Record(u, name, size, from, to, Some(p), None, None)
      }
      case Answer => (file |@| url |@| question |@| answer) { (f, u, q, a) =>
        val (name, size) = prepare(f, u)
        new Record(u, name, size, from, to, None, Some(q), Some(a))
      }
    }
  }

}