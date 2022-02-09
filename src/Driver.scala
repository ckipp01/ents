import java.io.File

import coursierapi.Dependency
import coursierapi.Fetch

import dotty.tools.dotc.interactive.Interactive
import dotty.tools.dotc.interactive.InteractiveDriver
import dotty.tools.dotc.parsing.Parsers.Parser
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.util.SourcePosition
import dotty.tools.dotc.util.Spans

import scala.jdk.CollectionConverters.*
import dotty.tools.dotc.printing.SyntaxHighlighting
import dotty.tools.dotc.core.Contexts.Context
import scala.annotation.transparentTrait

object Driver:

  def descriptionBuilder(description: String): StringBuilder =
    val sb = new StringBuilder
    sb ++= "Description:"
    sb ++= System.lineSeparator
    sb ++= description
    sb ++= System.lineSeparator

  // TODO I added this in to check something and then realized this would be a
  // great addition to be able to show a snippet as an untyped tree and also a
  // typed tree. There aren't helper methods for untyped trees like there are
  // for typed ones so we might have to make a untyped version of things like
  // pathTo so we can try to show the pos that we are pointing to with <||>.
  def showUntypedTree(example: Example) =
    val (pos, code) = extractPos(example.snippet)
    val parser = new Parser(pos.source)(using driver.currentCtx)
    val compilerTree = parser.parse()
    pprint.log(example)

  def showTpdTree(example: Example): Unit =
    val sb = descriptionBuilder(example.description)
    given Context = driver.currentCtx
    val (pos, code) = extractPos(example.snippet)
    val diagnostics = driver.run(uri, pos.source)
    assert(
      diagnostics.isEmpty,
      s"""|Code snippet must be able to compile for this to work.
          |
          |${diagnostics.mkString("\n")}""".stripMargin
    )

    val trees = driver.openedTrees(uri)
    val path = Interactive.pathTo(trees, pos)
    val headTree = path.headOption

    val highlighted = SyntaxHighlighting.highlight(example.snippet)

    sb ++= "Code example:"
    sb ++= System.lineSeparator
    sb ++= highlighted
    sb ++= System.lineSeparator

    headTree match
      case Some(tree) =>
        sb ++= "Minimal Tree:"
        sb ++= System.lineSeparator
        sb ++= tree.toString
        sb ++= System.lineSeparator
        sb ++= System.lineSeparator
        sb ++= "Verbose Tree:"
        sb ++= System.lineSeparator
        sb ++= pprint.tokenize(tree).mkString
        sb ++= System.lineSeparator
      case None =>
        sb ++= "Looks like pathTo didn't return a tree. Just printing out all the trees."
        sb ++= System.lineSeparator
        sb ++= pprint.tokenize(trees).mkString
        sb ++= System.lineSeparator
    end match

    println(sb)
  end showTpdTree

  private val filename = s"${this.toString}.scala"
  private val uri = java.net.URI.create(s"file:///$filename")

  private val driver =
    val fetch = Fetch.create()

    fetch.addDependencies(
      Dependency.of("org.scala-lang", "scala3-library_3", "3.1.1")
    )

    val extraLibraries = fetch
      .fetch()
      .asScala
      .map(_.toPath())
      .toSeq

    new InteractiveDriver(
      List(
        "-classpath",
        extraLibraries.mkString(File.pathSeparator)
      )
    )
  end driver

  private def extractPos(snippet: String) =
    val startIndex = snippet.indexOf("<|")
    val beginningStripped = snippet.replace("<|", "")
    val endIndex = beginningStripped.indexOf("|>")
    val cleanCode = beginningStripped.replace("|>", "")

    val span =
      if startIndex == endIndex then Spans.Span(startIndex)
      else Spans.Span(startIndex, endIndex)

    val sourceFile = SourceFile.virtual(uri.toString, cleanCode)
    val pos = new SourcePosition(sourceFile, span)

    (pos, cleanCode)
  end extractPos

end Driver
