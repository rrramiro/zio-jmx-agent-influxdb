package org.json4s
package zio

trait Lifting { this: Types =>

  implicit class Func1ToJSON[A: JSONR, R](z: A => R) {
    def applyJSON(a: JValue => Result[A]): JValue => Result[R] =
      (json: JValue) => a(json).map(z)
  }

  implicit class Func2ToJSON[A: JSONR, B: JSONR, R](z: (A, B) => R) {
    def applyJSON(
        a: JValue => Result[A],
        b: JValue => Result[B]
    ): JValue => Result[R] =
      (json: JValue) => (a(json), b(json)).map2(z)
  }

  implicit class Func3ToJSON[A: JSONR, B: JSONR, C: JSONR, R](
      z: (A, B, C) => R
  ) {
    def applyJSON(
        a: JValue => Result[A],
        b: JValue => Result[B],
        c: JValue => Result[C]
    ): JValue => Result[R] =
      (json: JValue) => (a(json), b(json), c(json)).map3(z)
  }

  implicit class Func4ToJSON[A: JSONR, B: JSONR, C: JSONR, D: JSONR, R](
      z: (A, B, C, D) => R
  ) {
    def applyJSON(
        a: JValue => Result[A],
        b: JValue => Result[B],
        c: JValue => Result[C],
        d: JValue => Result[D]
    ): JValue => Result[R] =
      (json: JValue) => (a(json), b(json), c(json), d(json)).map4(z)
  }

  implicit class Func5ToJSON[
      A: JSONR,
      B: JSONR,
      C: JSONR,
      D: JSONR,
      E: JSONR,
      R
  ](z: (A, B, C, D, E) => R) {
    def applyJSON(
        a: JValue => Result[A],
        b: JValue => Result[B],
        c: JValue => Result[C],
        d: JValue => Result[D],
        e: JValue => Result[E]
    ): JValue => Result[R] =
      (json: JValue) => (a(json), b(json), c(json), d(json), e(json)).map5(z)
  }

  implicit class Func6ToJSON[
      A: JSONR,
      B: JSONR,
      C: JSONR,
      D: JSONR,
      E: JSONR,
      F: JSONR,
      R
  ](z: (A, B, C, D, E, F) => R) {
    def applyJSON(
        a: JValue => Result[A],
        b: JValue => Result[B],
        c: JValue => Result[C],
        d: JValue => Result[D],
        e: JValue => Result[E],
        f: JValue => Result[F]
    ): JValue => Result[R] =
      (json: JValue) =>
        (a(json), b(json), c(json), d(json), e(json), f(json)).map6(z)
  }

  implicit class Func7ToJSON[
      A: JSONR,
      B: JSONR,
      C: JSONR,
      D: JSONR,
      E: JSONR,
      F: JSONR,
      G: JSONR,
      R
  ](z: (A, B, C, D, E, F, G) => R) {
    def applyJSON(
        a: JValue => Result[A],
        b: JValue => Result[B],
        c: JValue => Result[C],
        d: JValue => Result[D],
        e: JValue => Result[E],
        f: JValue => Result[F],
        g: JValue => Result[G]
    ): JValue => Result[R] =
      (json: JValue) =>
        (a(json), b(json), c(json), d(json), e(json), f(json), g(json)).map7(z)
  }

  implicit class Func8ToJSON[
      A: JSONR,
      B: JSONR,
      C: JSONR,
      D: JSONR,
      E: JSONR,
      F: JSONR,
      G: JSONR,
      H: JSONR,
      R
  ](z: (A, B, C, D, E, F, G, H) => R) {
    def applyJSON(
        a: JValue => Result[A],
        b: JValue => Result[B],
        c: JValue => Result[C],
        d: JValue => Result[D],
        e: JValue => Result[E],
        f: JValue => Result[F],
        g: JValue => Result[G],
        h: JValue => Result[H]
    ): JValue => Result[R] =
      (json: JValue) =>
        (a(json), b(json), c(json), d(json), e(json), f(json), g(json), h(json))
          .map8(z)
  }
}
