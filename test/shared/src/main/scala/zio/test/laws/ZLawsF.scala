/*
 * Copyright 2019-2020 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.test.laws

import zio.ZIO
import zio.test.{ check, checkM, Gen, TestResult }

object ZLawsF {

  sealed trait Covariant[-CapsF[_[+_]], -Caps[_], -R] { self =>

    /**
     * Test that values of type `F[+_]` satisfy the laws using the specified
     * function to construct a generator of `F[A]` values given a generator of
     * `A` values.
     */
    def run[R1 <: R, F[+_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult]

    /**
     * Combine these laws with the specified laws to produce a set of laws that
     * require both sets of laws to be satisfied.
     */
    def +[CapsF1[x[+_]] <: CapsF[x], Caps1[x] <: Caps[x], R1 <: R](
      that: Covariant[CapsF1, Caps1, R1]
    ): Covariant[CapsF1, Caps1, R1] =
      Covariant.Both(self, that)
  }

  object Covariant {

    private final case class Both[-CapsF[_[+_]], -Caps[_], -R](
      left: Covariant[CapsF, Caps, R],
      right: Covariant[CapsF, Caps, R]
    ) extends Covariant[CapsF, Caps, R] {
      final def run[R1 <: R, F[+_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        left.run(genF, gen).zipWith(right.run(genF, gen))(_ && _)
    }

    /**
     * Constructs a law from a pure function taking a single parameter.
     */
    abstract class Law1[-CapsF[_[+_]], -Caps[_]](label: String) extends Covariant[CapsF, Caps, Any] { self =>
      def apply[F[+_]: CapsF, A: Caps](fa: F[A]): TestResult
      final def run[R, F[+_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen))(apply(_).map(_.label(label)))
    }

    /**
     * Constructs a law from an effectual function taking a single parameter.
     */
    abstract class Law1M[-CapsF[_[+_]], -Caps[_], -R](label: String) extends Covariant[CapsF, Caps, R] { self =>
      def apply[F[+_]: CapsF, A: Caps](fa: F[A]): ZIO[R, Nothing, TestResult]
      final def run[R1 <: R, F[+_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        checkM(genF(gen))(apply(_).map(_.map(_.label(label))))
    }

    /**
     * Constructs a law from a pure function taking two parameters.
     */
    abstract class Law2[-CapsF[_[+_]], -Caps[_]](label: String) extends Covariant[CapsF, Caps, Any] { self =>
      def apply[F[+_]: CapsF, A: Caps, B: Caps](fa: F[A], fb: F[B]): TestResult
      final def run[R, F[+_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen), genF(gen))(apply(_, _).map(_.label(label)))
    }

    /**
     * Constructs a law from an effectual function taking two parameters.
     */
    abstract class Law2M[-CapsF[_[+_]], -Caps[_], -R](label: String) extends Covariant[CapsF, Caps, R] { self =>
      def apply[F[+_]: CapsF, A: Caps, B: Caps](fa: F[A], fb: F[B]): ZIO[R, Nothing, TestResult]
      final def run[R1 <: R, F[+_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        checkM(genF(gen), genF(gen))(apply(_, _).map(_.map(_.label(label))))
    }

    /**
     * Constructs a law from a pure function taking three parameters.
     */
    abstract class Law3[-CapsF[_[+_]], -Caps[_]](label: String) extends Covariant[CapsF, Caps, Any] { self =>
      def apply[F[+_]: CapsF, A: Caps, B: Caps, C: Caps](fa: F[A], fb: F[B], fc: F[C]): TestResult
      final def run[R, F[+_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen), genF(gen), genF(gen))(apply(_, _, _).map(_.label(label)))
    }

    /**
     * Constructs a law from an effectual function taking three parameters.
     */
    abstract class Law3M[-CapsF[_[+_]], -Caps[_], -R](label: String) extends Covariant[CapsF, Caps, R] { self =>
      def apply[F[+_]: CapsF, A: Caps, B: Caps, C: Caps](fa: F[A], fb: F[B], fc: F[C]): ZIO[R, Nothing, TestResult]
      final def run[R1 <: R, F[+_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        checkM(genF(gen), genF(gen), genF(gen))(apply(_, _, _).map(_.map(_.label(label))))
    }

    /**
     * Constructs a law from a pure function taking three parameters.
     */
    abstract class Law3Function[-CapsF[_[+_]], -Caps[_]](label: String) extends Covariant[CapsF, Caps, Any] { self =>
      def apply[F[+_]: CapsF, A: Caps, B: Caps, C: Caps](fa: F[A], f: A => B, g: B => C): TestResult
      final def run[R, F[+_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen), Gen.function(gen), Gen.function(gen))(apply(_, _, _).map(_.label(label)))
    }
  }

  sealed trait Contravariant[-CapsF[_[-_]], -Caps[_], -R] { self =>

    /**
     * Test that values of type `F[+_]` satisfy the laws using the specified
     * function to construct a generator of `F[A]` values given a generator of
     * `A` values.
     */
    def run[R1 <: R, F[-_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult]

    /**
     * Combine these laws with the specified laws to produce a set of laws that
     * require both sets of laws to be satisfied.
     */
    def +[CapsF1[x[-_]] <: CapsF[x], Caps1[x] <: Caps[x], R1 <: R](
      that: Contravariant[CapsF1, Caps1, R1]
    ): Contravariant[CapsF1, Caps1, R1] =
      Contravariant.Both(self, that)
  }

  object Contravariant {

    private final case class Both[-CapsF[_[-_]], -Caps[_], -R](
      left: Contravariant[CapsF, Caps, R],
      right: Contravariant[CapsF, Caps, R]
    ) extends Contravariant[CapsF, Caps, R] {
      final def run[R1 <: R, F[-_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        left.run(genF, gen).zipWith(right.run(genF, gen))(_ && _)
    }

    /**
     * Constructs a law from a pure function taking a single parameter.
     */
    abstract class Law1[-CapsF[_[-_]], -Caps[_]](label: String) extends Contravariant[CapsF, Caps, Any] { self =>
      def apply[F[-_]: CapsF, A: Caps](fa: F[A]): TestResult
      final def run[R, F[-_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen))(apply(_).map(_.label(label)))
    }

    /**
     * Constructs a law from an effectual function taking a single parameter.
     */
    abstract class Law1M[-CapsF[_[-_]], -Caps[_], -R](label: String) extends Contravariant[CapsF, Caps, R] { self =>
      def apply[F[-_]: CapsF, A: Caps](fa: F[A]): ZIO[R, Nothing, TestResult]
      final def run[R1 <: R, F[-_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        checkM(genF(gen))(apply(_).map(_.map(_.label(label))))
    }

    /**
     * Constructs a law from a pure function taking two parameters.
     */
    abstract class Law2[-CapsF[_[-_]], -Caps[_]](label: String) extends Contravariant[CapsF, Caps, Any] { self =>
      def apply[F[-_]: CapsF, A: Caps, B: Caps](fa: F[A], fb: F[B]): TestResult
      final def run[R, F[-_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen), genF(gen))(apply(_, _).map(_.label(label)))
    }

    /**
     * Constructs a law from an effectual function taking two parameters.
     */
    abstract class Law2M[-CapsF[_[-_]], -Caps[_], -R](label: String) extends Contravariant[CapsF, Caps, R] { self =>
      def apply[F[-_]: CapsF, A: Caps, B: Caps](fa: F[A], fb: F[B]): ZIO[R, Nothing, TestResult]
      final def run[R1 <: R, F[-_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        checkM(genF(gen), genF(gen))(apply(_, _).map(_.map(_.label(label))))
    }

    /**
     * Constructs a law from a pure function taking three parameters.
     */
    abstract class Law3[-CapsF[_[-_]], -Caps[_]](label: String) extends Contravariant[CapsF, Caps, Any] { self =>
      def apply[F[-_]: CapsF, A: Caps, B: Caps, C: Caps](fa: F[A], fb: F[B], fc: F[C]): TestResult
      final def run[R, F[-_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen), genF(gen), genF(gen))(apply(_, _, _).map(_.label(label)))
    }

    /**
     * Constructs a law from an effectual function taking three parameters.
     */
    abstract class Law3M[-CapsF[_[-_]], -Caps[_], -R](label: String) extends Contravariant[CapsF, Caps, R] { self =>
      def apply[F[-_]: CapsF, A: Caps, B: Caps, C: Caps](fa: F[A], fb: F[B], fc: F[C]): ZIO[R, Nothing, TestResult]
      final def run[R1 <: R, F[-_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        checkM(genF(gen), genF(gen), genF(gen))(apply(_, _, _).map(_.map(_.label(label))))
    }
  }

  sealed trait Invariant[-CapsF[_[_]], -Caps[_], -R] { self =>

    /**
     * Test that values of type `F[+_]` satisfy the laws using the specified
     * function to construct a generator of `F[A]` values given a generator of
     * `A` values.
     */
    def run[R1 <: R, F[_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult]

    /**
     * Combine these laws with the specified laws to produce a set of laws that
     * require both sets of laws to be satisfied.
     */
    def +[CapsF1[x[_]] <: CapsF[x], Caps1[x] <: Caps[x], R1 <: R](
      that: Invariant[CapsF1, Caps1, R1]
    ): Invariant[CapsF1, Caps1, R1] =
      Invariant.Both(self, that)
  }

  object Invariant {

    private final case class Both[-CapsF[_[_]], -Caps[_], -R](
      left: Invariant[CapsF, Caps, R],
      right: Invariant[CapsF, Caps, R]
    ) extends Invariant[CapsF, Caps, R] {
      final def run[R1 <: R, F[_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        left.run(genF, gen).zipWith(right.run(genF, gen))(_ && _)
    }

    /**
     * Constructs a law from a pure function taking a single parameter.
     */
    abstract class Law1[-CapsF[_[_]], -Caps[_]](label: String) extends Invariant[CapsF, Caps, Any] { self =>
      def apply[F[_]: CapsF, A: Caps](fa: F[A]): TestResult
      final def run[R, F[_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen))(apply(_).map(_.label(label)))
    }

    /**
     * Constructs a law from an effectual function taking a single parameter.
     */
    abstract class Law1M[-CapsF[_[_]], -Caps[_], -R](label: String) extends Invariant[CapsF, Caps, R] { self =>
      def apply[F[_]: CapsF, A: Caps](fa: F[A]): ZIO[R, Nothing, TestResult]
      final def run[R1 <: R, F[_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        checkM(genF(gen))(apply(_).map(_.map(_.label(label))))
    }

    /**
     * Constructs a law from a pure function taking two parameters.
     */
    abstract class Law2[-CapsF[_[_]], -Caps[_]](label: String) extends Invariant[CapsF, Caps, Any] { self =>
      def apply[F[_]: CapsF, A: Caps, B: Caps](fa: F[A], fb: F[B]): TestResult
      final def run[R, F[_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen), genF(gen))(apply(_, _).map(_.label(label)))
    }

    /**
     * Constructs a law from an effectual function taking two parameters.
     */
    abstract class Law2M[-CapsF[_[_]], -Caps[_], -R](label: String) extends Invariant[CapsF, Caps, R] { self =>
      def apply[F[_]: CapsF, A: Caps, B: Caps](fa: F[A], fb: F[B]): ZIO[R, Nothing, TestResult]
      final def run[R1 <: R, F[_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        checkM(genF(gen), genF(gen))(apply(_, _).map(_.map(_.label(label))))
    }

    /**
     * Constructs a law from a pure function taking three parameters.
     */
    abstract class Law3[-CapsF[_[_]], -Caps[_]](label: String) extends Invariant[CapsF, Caps, Any] { self =>
      def apply[F[_]: CapsF, A: Caps, B: Caps, C: Caps](fa: F[A], fb: F[B], fc: F[C]): TestResult
      final def run[R, F[_]: CapsF, A: Caps](genF: GenF[R, F], gen: Gen[R, A]): ZIO[R, Nothing, TestResult] =
        check(genF(gen), genF(gen), genF(gen))(apply(_, _, _).map(_.label(label)))
    }

    /**
     * Constructs a law from an effectual function taking three parameters.
     */
    abstract class Law3M[-CapsF[_[_]], -Caps[_], -R](label: String) extends Invariant[CapsF, Caps, R] { self =>
      def apply[F[_]: CapsF, A: Caps, B: Caps, C: Caps](fa: F[A], fb: F[B], fc: F[C]): ZIO[R, Nothing, TestResult]
      final def run[R1 <: R, F[_]: CapsF, A: Caps](genF: GenF[R1, F], gen: Gen[R1, A]): ZIO[R1, Nothing, TestResult] =
        checkM(genF(gen), genF(gen), genF(gen))(apply(_, _, _).map(_.map(_.label(label))))
    }
  }
}
