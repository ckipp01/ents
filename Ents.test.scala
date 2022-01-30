//> using lib "org.scalameta::munit::1.0.0-M1"

class EntsTests extends munit.FunSuite:

  // This is just a sanity check to ensure all the snippets are valid, and that
  // we can call show for both typed and untyped trees.
  Ents.values.foreach { tree =>
    test(tree.toString + " typed with position") {
      tree.showTpdTree()
    }

    test(tree.toString + " untyped full tree") {
      tree.showUntypedTree()
    }
  }

end EntsTests
