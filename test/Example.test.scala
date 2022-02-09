//> using lib "org.scalameta::munit::1.0.0-M1"

class ExampleTests extends munit.FunSuite:

  // This is just a sanity check to ensure all the snippets are valid, and that
  // we can call show for both typed and untyped trees.
  Example.values.foreach { example =>
    test(example.toString + " typed with position") {
      Driver.showTpdTree(example)
    }

    test(example.toString + " untyped full tree") {
      Driver.showUntypedTree(example)
    }
  }

end ExampleTests
