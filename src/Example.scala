///////////////////////////////////////////////////////////////////////////////
// NOTE: Most of this information came right from a lecture that Dmitry
// Petrashko did called Dotty Internals 1: Trees & Symbols. You can find the
// video online at. If watching to learn is more your thing, then I can highly
// recommend the video, and you can use Ents as a way to follow along with the
// lecture and to give a demonstration of each type of Tree that is covered.
// https://youtu.be/yYd-zuDd3S8
///////////////////////////////////////////////////////////////////////////////

enum Example(val description: String, val snippet: String):

  ////////////////////////
  // Types of Treees
  ////////////////////////

  case AppliedTypeTree
      extends Example(
        """|An AppliedTypeTree is used when you have a type that is being
           |applied and not just an Ident.
           |
           |These end up being nested with more complex signatures. Take the
           |below |example, the List[List[Int]] will have the tpt as an Ident of
           |List, but the args |will also be another AppliedTypeTree of another
           |List.
           |""".stripMargin,
        """|def foo(a: <|List[List[Int]]|>) = ()
          |""".stripMargin
      )

  case Apply
      extends Example(
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
           |val added = foo.<|plus(1)(2)|>
           |""".stripMargin
      )

  // TODO I thought the parser would show this...
  case Annotated
      extends Example(
        """|WARNING: The output of this isn't what I expect yet.
           |
           |Used when an @annotation is used.
           |
           |NOTE: That after typechecking annotations are moved to the
           |`denot.annotations`, so look at this with --untyped.
           |""".stripMargin,
        """|import scala.annotation.nowarn
           |
           |<|@nowarn|>
           |class Foo
           |""".stripMargin
      )

  case Assign
      extends Example(
        """|If you have a mutable variable and then you assign a value to that
           |variable the Assign tree will be used.
           |""".stripMargin,
        """|class Foo:
           |  var foo = 0
           |  def update(a: Int) = <|foo = a|>
           |""".stripMargin
      )

  case Block
      extends Example(
        """|Block is a tree relating to control flow. For example if you have a
           |block "{ }", do something things, and then return something, that
           |entire thing will be a Block. This is represented by a series of
           |statements and then that actual expression that is returned.
           |
           |So in the example, there is a single statement and then the
           |expression.
           |""".stripMargin,
        """|val foo = <|{
           |  val a = 1
           |  a + 1
           |}|>
           |""".stripMargin
      )

  case ByNameTypeTree
      extends Example(
        """|When you have a TypeDef with a match type the body of the TypeDef
           |will be using a MatchTypeTree.
           |""".stripMargin,
        """|type Elem[X] = <|X match
           |  case String => Char
           |  case Iterable[t] => t|>
           |""".stripMargin
      )

  case CaseDef
      extends Example(
        """|A CaseDef are the actual cases inside of a Match tree.
           |
           |The pattern is what "shape" you are looking for.
           |The guard is an optional tree if there is an "if".
           |The body is what is to executed when you enter that case.
           |""".stripMargin,
        """|def foo(a: Int) = a match
           |  <|case 1 => "you got a one!"|>
           |  case _ => "you didn't get a one!"
           |""".stripMargin
      )

  case Closure
      extends Example(
        """|A Closure is a tree that more or less defines a lambda.
           |
           |The meth part of this tree is referring to the method that will be
           |silintly created to define the closure.
           |For example, take the following:
           |List(1).map(x => x * 2)
           |
           |The x * 2 will actually be extracted into:
           |def annon$(x) = x * 2
           |List(1).map(annon$)
           |
           |The tpt tells you what type of Single Abstract Method type that it
           |implements.
           |
           |Also note that the <||> is not just around the closure where you'd
           |think it should be because if we do that, you would only actually
           |see the DefDef for the $anonfun. The Closure will actually be in the
           |expr of the Apply args.
           |""".stripMargin,
        """|val foo = List(1).<|map(x => x * 2)|>
           |""".stripMargin
      )

  case DefDef
      extends Example(
        """|Used when see a method being defined DefDef is used.
           |""".stripMargin,
        """|<|def foo() = "foo"|>
           |""".stripMargin
      )

  case Ident
      extends Example(
        """|Ident is a type of RefTree and RefTrees are used to references
           |already existing definitions. "foo" in this example is an Ident.
           |""".stripMargin,
        """|case class Foo(a: Int)
           |val foo = Foo(3)
           |val num = <|foo|>.a
           |""".stripMargin
      )

  case If
      extends Example(
        """|Represents an if expression including the full condition (that can
           |be complex), the thenp and also the elsep.
           |""".stripMargin,
        """|val foo = <|if (true) 1 else 0|>
           |""".stripMargin
      )

  case Import
      extends Example(
        """|Represents an import statement.
           |""".stripMargin,
        """|<|import java.time.Instant|>
           |""".stripMargin
      )

  // TODO I need to figure out how to do this one for example in the snippet
  // now is just a ValDef and I'm assuming that the actual Inlined is in the
  // tpt, so I need to figure out how to show this.
  // https://docs.scala-lang.org/sips/inline-meta.html
  // case Inlined
  //    extends Example(
  //      """|The result of inlining a call.
  //         |""".stripMargin,
  //      """|<|inline val x = 4|>
  //         |""".stripMargin
  //    )

  case LambdaTypeTree
      extends Example(
        """|When you have TypeDef where the rhs is a type lambda the
           |LambdaTypeTree is used.
           |""".stripMargin,
        """|type foo = <|[X] =>> List[X]|>
           |""".stripMargin
      )

  case Literal
      extends Example(
        """|Literal is a constant. For example just 1 or "hi"".
           |""".stripMargin,
        """|val foo = <|1|>
           |""".stripMargin
      )

  case Match
      extends Example(
        """|A Match tree represents the entire match expression including all
           |of the cases.
           |
           |The selector is what you are matching on and the cases are the
           |actual cases you are matching on.
           |""".stripMargin,
        """|def foo(a: Int) = <|a match
           |  case 1 => "you got a one!"
           |  case _ => "you didn't get a one!"|>
           |""".stripMargin
      )

  case MatchTypeTree
      extends Example(
        """|When you have a TypeDef with a match type the body of the TypeDef
           |will be using a MatchTypeTree.
           |""".stripMargin,
        """|type Elem[X] = <|X match
           |  case String => Char
           |  case Iterable[t] => t|>
           |""".stripMargin
      )

  case NamedArg
      extends Example(
        """|NamedArgs are used when using a by name argument.
           |
           |NOTE: Keep in mind that in untyped trees the order is
           |as written. However, when that tree becomes typed, it
           |will be changed to be in the correct order. You will 
           |however still be able to see it was passed in by name.
           |""".stripMargin,
        """|case class Foo(a: Int, b: Int)
           |val foo = Foo(<|b|> = 2, a = 1)
           |""".stripMargin
      )

  case New
      extends Example(
        """|New refers to a type in which you are constructing.
           |""".stripMargin,
        """|class Foo
           |val foo = <|new|> Foo
           |""".stripMargin
      )

  // TODO this one doesn't return what I thought it would, is the PackageDef
  // already just gone? Try this with an untyped Tree.
  case PackageDef
      extends Example(
        """|WARNING: This tree isn't fully correct yet.
           |
           |Defines a package which has classes inside.
           |
           |So when running the code snippet with -Vprint:all -Yplain-printer I see:
           |[[syntax trees at end of              constructors]] // package.scala
           |PackageDef(Ident(foo), List(
           |  TypeDef(Foo,
           |    Template(
           |      DefDef(<init>, List(List()), TypeExample(),
           |        Block(List(
           |          Apply(Select(Super(This(Ident(Foo)), Ident()), <init>), List())
           |        ), Literal(()))
           |      )
           |    , List(TypeExample()), ValDef(_, Thicket(List()), Thicket(List())), List())
           |  )
           |))
           |
           |However looking at the opened trees for this I just see
           |SourceTree... need to figure this out.
           |""".stripMargin,
        """|<|package foo|>
           |
           |class Foo
           |""".stripMargin
      )

  case Return
      extends Example(
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
           |  <|return x|>
           |""".stripMargin
      )

  case Select
      extends Example(
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
           |val num = foo.<|a|>
           |""".stripMargin
      )

  case SeqLiteral
      extends Example(
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
           |val value = foo(<|1, 2, 3|>)
           |""".stripMargin
      )

  case SingletonTypeTree
      extends Example(
        """|Used in a singleton type of ref.type.
           |""".stripMargin,
        """|val foo: String = "foo"
           |type fooString = <|foo.type|>
           |
           |""".stripMargin
      )

  case Super
      extends Example(
        "Super is referencing the super class of the current class.",
        """|trait A:
           |  def a() = 1
           |class Foo extends A:
           |  val b = <|super|>.a()
           |""".stripMargin
      )

  case Template
      extends Example(
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
        """|class Foo(a: Int):<|
           |  val foo = 1|>
           |""".stripMargin
      )

  case Thicket
      extends Example(
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
           |  <|case num if num > 0 => "you something positive!"
           |  case _ => "negative!"|>
           |""".stripMargin
      )

  case This
      extends Example(
        "This is referencing the current class.",
        """|class Foo:
           |  val a = <|this|>
           |""".stripMargin
      )

  case Try
      extends Example(
        """|A Try tree is a tree that surrounds an entire Try expresion
           |including the expression, the cases, and the finalizer. 
           |  
           |""".stripMargin,
        """|import java.lang.Throwable
           |val foo =
           |  <|try
           |    println("pretend we are reading a file here")
           |  catch
           |    case _: Throwable => println("something went wrong")
           |  finally
           |    println("you did it!")|>
           |""".stripMargin
      )

  case TypeDef
      extends Example(
        """|Used when you are defining a type. This type can either be a clases
           |or a local type.
           |
           |The way to tell if this is a class or a type is to look a at the rhs.
           |For classes it will be a template. However a bettery way to do this is
           |to look at the tpe.
           |""".stripMargin,
        """|<|type A = Int|>
           |""".stripMargin
      )

  case Typed
      extends Example(
        """|Typed trees are used when you have a type description. An
           |example is when you are widening a type like you see in the
           |example where we have an Int but are widening to Any.
           |""".stripMargin,
        """|val example = <|1: Any|>
           |""".stripMargin
      )

  case TypeApply
      extends Example(
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
           |val added = <|foo.plus(1)|>
           |""".stripMargin
      )

  case ValDef
      extends Example(
        """|Used when you see a val definition (a field or local variable).
           |
           |The preRhs has a LazyTree where Dotty can postpone computation of.
           |""".stripMargin,
        """|<|val foo = "foo"|>
           |""".stripMargin
      )

  case WhileDo
      extends Example(
        """|Used in a while loop.
           |
           |while (cond) { body }
           |""".stripMargin,
        """|object Foo:
           |  var foo = 0
           |  <|while foo < 3 do foo +=1|>
           |""".stripMargin
      )

end Example
