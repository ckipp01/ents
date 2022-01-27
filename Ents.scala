import java.io.File
import java.net.URI

import coursierapi.Dependency
import coursierapi.Fetch

import dotty.tools.dotc.interactive.Interactive
import dotty.tools.dotc.interactive.InteractiveDriver
import dotty.tools.dotc.parsing.Parsers.Parser
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.util.SourcePosition
import dotty.tools.dotc.util.Spans

import scala.jdk.CollectionConverters.*

///////////////////////////////////////////////////////////////////////////////
// NOTE: Most of this information came right from a lecture that Dmitry
// Petrashko did called Dotty Internals 1: Trees & Symbols. You can find the
// video online at. If watching to learn is more your thing, then I can highly
// recommend the video, and you can use Ents as a way to follow along with the
// lecture and to give a demonstration of each type of Tree that is covered.
// https://youtu.be/yYd-zuDd3S8
///////////////////////////////////////////////////////////////////////////////

enum Ents(description: String, snippet: String):

  // ////////////////////
  // Types of Treees
  // ////////////////////
  case Apply
      extends Ents(
        """|When calling a method on a class you still have a Select, but
           |it's also now wrapped in an Apply.
           |
           |In the example the plus is still a Select, but since we are
           |passing in arguments and it's a method you'll see it wrapped
           |in an Apply.
           |
           |NOTE: This example also shows what it looks like when you have
           |multiple parameter groups meaning that your Apply is wrapped in
           |another Apply.
           |""".stripMargin,
        """|class Foo:
           |  def plus(a: Int)(b: Int) = a + b
           |
           |val foo = new Foo
           |val added = foo.<<plus(1)(2)>>
           |""".stripMargin
      )

  case Assign
      extends Ents(
        """|If you have a mutable variable and then you assign a value to that
           |variable the Assign tree will be used.
           |""".stripMargin,
        """|class Foo:
           |  var foo = 0
           |  def update(a: Int) = <<foo = a>>
           |""".stripMargin
      )

  case Block
      extends Ents(
        """|Block is a tree relating to control flow. For example if you have a
           |block "{ }", do something things, and then return something, that
           |entire thing will be a Block. This is represented by a series of
           |statements and then that actual expression that is returned.
           |
           |So in the example, there is a single statement and then the
           |expression.
           |""".stripMargin,
        """|val foo = <<{
           |  val a = 1
           |  a + 1
           |}>>
           |""".stripMargin
      )

  case CaseDef
      extends Ents(
        """|A CaseDef are the actual cases inside of a Match tree.
           |
           |The pattern is what "shape" you are looking for.
           |The guard is an optional tree if there is an "if".
           |The body is what is to executed when you enter that case.
           |""".stripMargin,
        """|def foo(a: Int) = a match
           |  <<case 1 => "you got a one!">>
           |  case _ => "you didn't get a one!"
           |""".stripMargin
      )

  // TODO I need to figure out the best way to show this because right now this
  // is showing a DefDef, not a Closure where I want it. I honestly just don't
  // know if maybe at this point where I'm looking at the trees the Closure no
  // longer exists and it's just a DefDef since it's been lifted?
  //
  // Even when just using the parser the args here aren't a Closure
  //        args = List(
  //        Function(
  //          args = List(ValDef(name = x, tpt = TypeTree, preRhs = Thicket(trees = List()))),
  //          body = InfixOp(
  //            left = Ident(name = x),
  //            op = Ident(name = *),
  //            right = Number(digits = "2", kind = Whole(radix = 10))
  //          )
  //        )
  //      )
  // case Closure
  //    extends Ents(
  //      """|A Closure is a tree that more or less defineds a lambda.
  //         |
  //         |The meth part of this tree is referring to the method that will be
  //         |silintly created to define the closure.
  //         |For example, take the following:
  //         |List(1).map(x => x * 2)
  //         |
  //         |The x * 2 will actually be extracted into:
  //         |def annon$(x) = x * 2
  //         |List(1).map(annon$)
  //         |
  //         |The tpt tells you what type of Single Abstract Method type that it
  //         |implements.
  //         |""".stripMargin,
  //      """|val foo = List(1).map(<<x => x * 2>>)
  //         |""".stripMargin
  //    )

  case Ident
      extends Ents(
        """|Ident is a type of RefTree and RefTrees are used to references
           |already existing definitions. "foo" in this example is an Ident.
           |""".stripMargin,
        """|case class Foo(a: Int)
           |val foo = Foo(3)
           |val num = <<foo>>.a
           |""".stripMargin
      )

  case If
      extends Ents(
        """|Represents an if expression including the full condition (that can
           |be complex), the thenp and also the elsep.
           |""".stripMargin,
        """|val foo = <<if (true) 1 else 0>>
           |""".stripMargin
      )

  case Match
      extends Ents(
        """|A Match tree represents the entire match expression including all
           |of the cases.
           |
           |The selector is what you are matching on and the cases are the
           |actual cases you are matching on.
           |""".stripMargin,
        """|def foo(a: Int) = <<a match
           |  case 1 => "you got a one!"
           |  case _ => "you didn't get a one!">>
           |""".stripMargin
      )

  case NamedArg
      extends Ents(
        """|NamedArgs are used when using a by name argument.
           |
           |NOTE: Keep in mind that in untyped trees the order is
           |as written. However, when that tree becomes typed, it
           |will be changed to be in the correct order. You will 
           |however still be able to see it was passed in by name.
           |""".stripMargin,
        """|case class Foo(a: Int, b: Int)
           |val foo = Foo(<<b>> = 2, a = 1)
           |""".stripMargin
      )

  case New
      extends Ents(
        """|New refers to a type in which you are constructing.
           |""".stripMargin,
        """|class Foo
           |val foo = <<new>> Foo
           |""".stripMargin
      )

  case Literal
      extends Ents(
        """|Literal is a constant. For example just 1 or "hi"".
           |""".stripMargin,
        """|val foo = <<1>>
           |""".stripMargin
      )

  case Return
      extends Ents(
        """|The Return tree signifies that are you finishing execution of a method.
           |
           |The method that you are going to be returning from is listed in the from.
           |If the from contains an empty tree it means you are finishing a
           |method that is the most enclosing one.
           |
           |The expr of this tree should be the same type as the type of the
           |method you are returning from.
           |""".stripMargin,
        """|def foo(): Int =
           |  val x = 3
           |  <<return x>>
           |""".stripMargin
      )

  case Select
      extends Ents(
        """|Select is a type of RefTree and RefTrees are used to references
           |already existing definitions. "a" in this example is a Select.
           |
           |NOTE: keep in mind that "a" is value here. If this was a method
           |that we'd be passing arguments to, then Foo would also be wrapped
           |in an Apply.
           |
           |There are also more specific types of Select trees such as
           | SelectWithSig.
           |""".stripMargin,
        """|case class Foo(a: Int)
           |val foo = Foo(3)
           |val num = foo.<<a>>
           |""".stripMargin
      )

  case Super
      extends Ents(
        "Super is referencing the super class of the current class.",
        """|trait A:
           |  def a() = 1
           |class Foo extends A:
           |  val b = <<super>>.a()
           |""".stripMargin
      )

  case This
      extends Ents(
        "This is referencing the current class.",
        """|class Foo:
           |  val a = <<this>>
           |""".stripMargin
      )

  case Try
      extends Ents(
        """|A Try tree is a tree that surrounds an entire Try expresion
           |including the expression, the cases, and the finalizer. 
           |  
           |""".stripMargin,
        """|import java.lang.Throwable
           |val foo =
           |  <<try
           |    println("pretend we are reading a file here")
           |  catch
           |    case _: Throwable => println("something went wrong")
           |  finally
           |    println("you did it!")>>
           |""".stripMargin
      )

  case Typed
      extends Ents(
        """|Typed trees are used when you have a type description. An
           |example is when you are widening a type like you see in the
           |example where we have an Int but are widening to Any.
           |""".stripMargin,
        """|val example = <<1: Any>>
           |""".stripMargin
      )

  case TypeApply
      extends Ents(
        """|When you have an Apply that also takes in a Type, you'll also
           |see a TypeApply in the tree.
           |
           |So in the example, when plus(1) is called, there is a TypeApply
           |that is also there instantiating the [A]. This one is hard to show
           |as a top level tree, so you'll see this example wrapped in the
           |Apply.
           |""".stripMargin,
        """|class Foo:
           |  def plus[A](a: A) = ()
           |
           |val foo = new Foo
           |val added = <<foo.plus(1)>>
           |""".stripMargin
      )

  // ///////////////////////////////////
  // Utilities to deal with the trees
  // ///////////////////////////////////

  def showDescription() =
    println(description)

  // TODO I added this in to check something and then realized this would be a
  // great addition to be able to show a snippet as an untyped tree and also a
  // typed tree. There aren't helper methods for untyped trees like there are
  // for typed ones so we might have to make a untyped version of things like
  // pathTo so we can try to show the pos that we are pointing to with <<>>.
  def showUntypedTree() =
    val (pos, code) = extractPos()
    val parser = new Parser(pos.source)(using driver.currentCtx)
    val tree = parser.parse()
    pprint.log(tree)

  def showTpdTree() =
    val (pos, code) = extractPos()
    val diagnostics = driver.run(uri, pos.source)
    assert(
      diagnostics.isEmpty,
      s"""|Code snippet must be able to compile for this to work.
          |
          |${diagnostics.mkString("\n")}""".stripMargin
    )
    val trees = driver.openedTrees(uri)
    val path = Interactive.pathTo(trees, pos)(using driver.currentCtx)
    val tree = path.head
    pprint.pprintln(s"""|
                        |Code example:
                        |
                        |$snippet
                        |""".stripMargin)
    pprint.pprintln(tree)
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
        "-color:never",
        "-classpath",
        extraLibraries.mkString(File.pathSeparator)
      )
    )
  end driver

  private def extractPos() =
    val startIndex = snippet.indexOf("<<")
    val beginningStripped = snippet.replace("<<", "")
    val endIndex = beginningStripped.indexOf(">>")
    val cleanCode = beginningStripped.replace(">>", "")

    val span =
      if startIndex == endIndex then Spans.Span(startIndex)
      else Spans.Span(startIndex, endIndex)

    val sourceFile = SourceFile.virtual(uri.toString, cleanCode)
    val pos = new SourcePosition(sourceFile, span)

    (pos, cleanCode)
  end extractPos

end Ents
