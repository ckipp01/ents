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
import dotty.tools.dotc.printing.SyntaxHighlighting
import dotty.tools.dotc.core.Contexts.Context

///////////////////////////////////////////////////////////////////////////////
// NOTE: Most of this information came right from a lecture that Dmitry
// Petrashko did called Dotty Internals 1: Trees & Symbols. You can find the
// video online at. If watching to learn is more your thing, then I can highly
// recommend the video, and you can use Ents as a way to follow along with the
// lecture and to give a demonstration of each type of Tree that is covered.
// https://youtu.be/yYd-zuDd3S8
///////////////////////////////////////////////////////////////////////////////

enum Ents(description: String, snippet: String):

  // /////////////////////
  // Types of Treees
  // /////////////////////

  // TODO figure this one out. I was under the assumption this would be an AndTypeTree
  // but this is actually an AppliedTypeTree
  // case AndTypeTree
  //     extends Ents(
  //       """|A TypeTree that is representing an and Type which is an
  //          |"intersection" of the required types.
  //          |""".stripMargin,
  //       """|def foo(a: <<Int & String>>) = ()
  //          |""".stripMargin
  //     )

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

  // TODO figure this one out.
  // case Annotated
  //    extends Ents(
  //      """|Used when an @annotation is used.
  //         |""".stripMargin,
  //      """|import scala.annotation.nowarn
  //         |
  //         |<<@nowarn>>
  //         |class Foo
  //         |""".stripMargin
  //    )

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
  // is showing a DefDef, not a Closure where I want it. But that also makese
  // sense a bit. You can see with with -Xprint:all
  //
  // ❯ scala3-compiler -Xprint:all test.scala
  // [[syntax trees at end of                    parser]] // test.scala
  // package <empty> {
  //  val foo = List(1).map(x => x * 2)
  // }
  //
  // [[syntax trees at end of                     typer]] // test.scala
  // package <empty> {
  //  final lazy module val test$package: test$package = new test$package()
  //  final module class test$package() extends Object() {
  //    this: test$package.type =>
  //    val foo: List[Int] =
  //      List.apply[Int]([1 : Int]*).map[Int](
  //        {
  //          def $anonfun(x: Int): Int = x.*(2)
  //          closure($anonfun)
  //        }
  //      )
  //  }
  // }
  // So the tree we are looking at here is actually just the `DefDef` of $anonfun
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

  case DefDef
      extends Ents(
        """|Used when see a method being defined DefDef is used.
           |""".stripMargin,
        """|<<def foo() = "foo">>
           |""".stripMargin
      )

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

  case Import
      extends Ents(
        """|Represents an import statement.
           |""".stripMargin,
        """|<<import java.time.Instant>>
           |""".stripMargin
      )

  // TODO I need to figure out how to do this one
  // for example in the snippet now is just a ValDef
  // https://docs.scala-lang.org/sips/inline-meta.html
  // case Inlined
  //    extends Ents(
  //      """|The result of inlining a call.
  //         |""".stripMargin,
  //      """|<<inline val x = 4>>
  //         |""".stripMargin
  //    )

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

  // TODO figure this one out. I was under the assumption this would be an OrTypeTree
  // but this is actually an AppliedTypeTree
  // case OrTypeTree
  //    extends Ents(
  //      """|A TypeTree that is representing an or type which is an
  //          |"union" of the required types.
  //          |""".stripMargin,
  //      """|def foo(a: <<Int | String>>) = ()
  //          |""".stripMargin
  //    )

  // TODO this one doesn't return what I thought it would, is the PackageDef
  // already just gone? Try this with an untyped Tree.
  case PackageDef
      extends Ents(
        """|Defines a package which has classes inside.
           |""".stripMargin,
        """|package <<foo>>
           |class Foo
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

  case SeqLiteral
      extends Ents(
        """|When you have a var arg there needs to be a way at the call site to
           |know that each arg |is actually pointing to the same arg in the method.
           |In this case a SeqLiteral will be used.
           |
           |At some point in the pipeline you will also see this as a
           |JavaSeqLiteral. The difference is the runtime representation. In
           |Scala they are Scala collections while in Java they are the
           |underlying JVM arrays.
           |""".stripMargin,
        """|def foo(a: Int*): Unit = ()
           |val value = foo(<<1, 2, 3>>)
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

  case Template
      extends Ents(
        """|A Template is used to define the body of a class.
           |
           |The constr refers to the constructor of the class. For example in:
           |
           |class Foo(a: Int) {
           |  val s = 1
           |}
           |
           |The constr DefDef will be refering to a.
           |
           |NOTE: That the self here in this tree is referring to the self
           |type, but later on this may not be there anymore so the reliable
           |way to get this is through types.
           |""".stripMargin,
        """|class Foo(a: Int):<<
           |  val foo = 1>>
           |""".stripMargin
      )

  case Thicket
      extends Ents(
        """|This is a helper tree that allows you to return multiple trees in a
           |location where you'd expect to return a singular one.
           |
           |This is often used in transform methods where you want to transform
           |a method and maybe you'll return two methods. However, if it's in a
           |place that expects a single tree you can then return a Thicket of
           |those two trees.
           |
           |It also often seems to be used when you have an empty tree. For
           |example in the example below we have two CaseDefs. The first one has
           |a guard and the second one doesn't. When you look at the CaseDef
           |trees you'll see the guard for the first one is an Apply, but the
           |guard for the second one is empty and noted by a
           |Thicket(trees = List()).
           |
           |""".stripMargin,
        """|def foo(a: Int) = a match
           |  <<case num if num > 0 => "you something positive!"
           |  case _ => "negative!">>
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

  case TypeDef
      extends Ents(
        """|Used when you are defining a type. This type can either be a clases
           |or a local type.
           |
           |The way to tell if this is a class or a type is to look a at the rhs.
           |For classes it will be a template. However a bettery way to do this is
           |to look at the tpe.
           |""".stripMargin,
        """|<<type A = Int>>
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

  case ValDef
      extends Ents(
        """|Used when you see a val definition (a field or local variable).
           |
           |The preRhs has a LazyTree where Dotty can postpone computation of.
           |""".stripMargin,
        """|<<val foo = "foo">>
           |""".stripMargin
      )

  //////////////////////////////////////
  // Utilities to deal with the trees
  //////////////////////////////////////

  def descriptionBuilder(): StringBuilder =
    val sb = new StringBuilder
    sb ++= "Description:"
    sb ++= System.lineSeparator
    sb ++= description
    sb ++= System.lineSeparator

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

  def showTpdTree(): Unit =
    val sb = descriptionBuilder()
    given Context = driver.currentCtx
    val (pos, code) = extractPos()
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

    val highlighted = SyntaxHighlighting.highlight(snippet)

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
        sb ++= trees.toString
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
