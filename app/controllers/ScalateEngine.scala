package controllers

import play.api.mvc.Controller
import play.twirl.api.{ Html, HtmlFormat }
import org.fusesource.scalate.TemplateEngine

trait ScalateEngine {
  self: Controller =>

  import play.api.Play
  import java.io.File

  lazy val engine = {
    lazy val path = Play.current.path

    val sourceDirectories = new File(path, "app") :: Nil
    val e = new TemplateEngine(sourceDirectories, "production")
    e.boot
    e
  }

  def render(template: String, attributes: Map[String, Any] = Map.empty): Html =
    HtmlFormat raw engine.layout(template, attributes)

  def render(template: String, attributes: (String, Any)*): Html =
    render(template, attributes.toMap)
}
