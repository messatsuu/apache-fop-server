package org.zilverline.fogit fetch originp

import grizzled.slf4j.Logging
import unfiltered.request._
import unfiltered.response._

/**
 * Standalone HTTP server using the http://unfiltered.databinder.net/
 * HTTP server toolkit.
 */
object FopServer extends Logging {
  val DefaultPort = 9999

  object Xsl extends Params.Extract("xsl", Params.first)
  object Xml extends Params.Extract("xml", Params.first)
  object PdfAProfile extends Params.Extract("pdf-a-profile", Params.first)

  val heapSize = Runtime.getRuntime().totalMemory();
  val heapMaxSize = Runtime.getRuntime().maxMemory();
  val heapFreeSize = Runtime.getRuntime().freeMemory();

  info(s"Heap size: ${heapSize}");
  info(s"Heap max size: ${heapMaxSize}");
  info(s"Heap free size: ${heapFreeSize}");

  val plan = unfiltered.filter.Planify {
    case GET(Path("/is-alive"))                           => Ok ~> ResponseString("Ok")

    case POST(Path("/pdf")) & Params(params) & Params(Xsl(xsl) & Xml(xml)) => {
      // Extract pdf-a-profile parameter with a default value
      val pdfAProfile = params.get("pdf-a-profile").flatMap(_.headOption).getOrElse(null)
      generate(PdfDocument(xsl, xml, pdfAProfile))
    }
  }

  def main(args: Array[String]): Unit = {
    val maxSize = args match {
      case Array(max) => max
      case Array() => "4000000"
    }

    System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize", maxSize)

    unfiltered.jetty.Http.apply(DefaultPort).filter(plan).run { server =>
      sys.addShutdownHook(server.stop)
    }
  }

  private def generate(document: PdfDocument) = try {
    Ok ~> ContentType("application/pdf") ~> ResponseBytes(document.render)
  } catch {
    case e: Exception =>
      error(f"Error creating PDF for $document: $e", e)
      InternalServerError ~> ResponseString("See apache-fop-server/logs for more detailed error message.")
  }
}
