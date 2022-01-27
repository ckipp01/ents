// using lib org.scalameta::munit::1.0.0-M1

class EntsTests extends munit.FunSuite:

  // This is just a sanity check to ensure all the snippets are valid
  Ents.values.foreach { tree =>
    test(tree.toString) {
      tree.showTpdTree()
    }
  }

end EntsTests
