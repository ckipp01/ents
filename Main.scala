//> using scala "3.1.1"
//> using lib "com.lihaoyi::pprint:0.7.1"
//> using lib "io.get-coursier:interface:1.0.6"
//> using lib "org.scala-lang::scala3-compiler:3.1.0"

// left off at 48:11
import scala.io.StdIn
import scala.util.Failure
import scala.util.Success
import scala.util.Try

@main def run(args: String*): Unit =
  args match
    case Seq("help") | Seq("--h") | Seq("-h") | Seq("--help") => help()
    case args =>
      val choice = choose()
      choice match
        case Success(ordinal) if ordinal >= Ents.values.size =>
          println(
            "Please choose a number that corresponds to the type of tree you want to see"
          )
        case Success(ordinal) =>
          val tree = Ents.fromOrdinal(ordinal)
          // TODO don't advertise this yet since it's currently just printing
          // out the entire untyped tree. Figure out a way to get the tree at
          // the position for untyped before putting this out there.
          if args.contains("--untpd") || args.contains("--untyped") then
            tree.showUntypedTree()
          else tree.showTpdTree()

        case Failure(_) =>
          println(
            "Please choose a number that corresponds to the type of tree you want to see"
          )
      end match

private def choose() =
  val choices = Ents.values
  println("Choose the type of tree you'd like to see:")
  choices.foreach { tree => println(s"[${tree.ordinal}] ${tree.toString}") }
  Try(StdIn.readLine("> ").toInt)

private def help() = println("""
      _-_
    /~~   ~~\
 /~~         ~~\
{               }
 \  _-     -_  /
   ~  \\ //  ~
_- -   | | _- _
  _ -  | |   -_
      // \\

Ents, shepherd of the trees.

Run this program without any arguments, and then choose a tree type.""")
