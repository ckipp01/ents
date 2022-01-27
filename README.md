# Ents

<img alt="Treebeard and the Ents by Timothy Ide" align="left" width="500" src="./ents.jpeg">

## Understanding Scala 3 Compiler Trees

Ents is a small project to help me learn the Scala 3 compiler trees. There is a
great [video lecture](https://youtu.be/yYd-zuDd3S8) by [Dmitry
Petrashko](https://twitter.com/darkdimius) that I learned a ton from, but I suck
at just listening and retaining. So this project was a way for me to take some
notes and demonstrate with code the Trees that are being covered.

As I learn more about Trees I'll continue to add and amend these to hopefully
give a thorough overview and help someone else understand Trees. This is heavily
a work in progress at the moment.

## How to run

Ents is a command-line application built with
[scala-cli](https://scala-cli.virtuslab.org/). To use it, just run `scala-cli
run .` and follow the instructions by choosing the type of tree you'd like to
see. You'll then get a brief description, a code sample, and a tree that most
closely encloses the `<<>>` markers.

```
❯ scala-cli run .
Choose the type of tree you'd like to see:
[0] Apply
[1] Assign
[2] Block
[3] CaseDef
[4] Ident
[5] If
[6] Match
[7] NamedArg
[8] New
[9] Literal
[10] Return
[11] Select
[12] Super
[13] This
[14] Try
[15] Typed
[16] TypeApply
> 7
NamedArgs are used when using a by name argument.

NOTE: Keep in mind that in untyped trees the order is
as written. However, when that tree becomes typed, it
will be changed to be in the correct order. You will
however still be able to see it was passed in by name.

"""
Code example:

case class Foo(a: Int, b: Int)
val foo = Foo(<<b>> = 2, a = 1)

"""
NamedArg(name = b, arg = Literal(const = ( = 2)))
```

<sub>_The image is "Treebeard and the Ents" by Timothy Ide_</sub>
