package perfectcuboid

import scala.compiletime.summonAll
import scala.deriving.Mirror

/** How a value is written as words on one line of a results file, and read back.
  *
  * A type class, with instances for single words (`Long`, or any type given `word`), and for
  * any case class whose fields have instances: `derives LineFormat` writes the fields in
  * order, separated by spaces, and reads them back in the same order. The compiler builds that
  * instance from the case class's `Mirror`, a description of its fields that Scala 3 provides
  * for every case class, so no format has to be written out field by field.
  */
trait LineFormat[A]:
  def words(a: A): List[String]

  /** The value at the start of `words`, and the words after it. */
  def read(words: List[String]): Option[(A, List[String])]

object LineFormat:

  def write[A](a: A)(using format: LineFormat[A]): String = format.words(a).mkString(" ")

  /** The value at the start of the line; any words after it are left alone. */
  def parse[A](line: String)(using format: LineFormat[A]): Option[A] =
    format.read(line.split(" +").toList.filter(_.nonEmpty)).map(_._1)

  /** A value written as a single word. */
  def word[A](show: A => String, readWord: String => Option[A]): LineFormat[A] = new:
    def words(a: A): List[String] = List(show(a))
    def read(words: List[String]): Option[(A, List[String])] = words match
      case first :: rest => readWord(first).map(_ -> rest)
      case Nil           => None

  given LineFormat[Long] = word(_.toString, _.toLongOption)

  /** What `derives LineFormat` calls. `inline` makes the compiler expand it where it is used,
    * when the field types are known, so that `summonAll` can find a format for each of them.
    */
  inline def derived[A](using mirror: Mirror.ProductOf[A]): LineFormat[A] =
    val fields = summonAll[Tuple.Map[mirror.MirroredElemTypes, LineFormat]].toList
    product(fields.asInstanceOf[List[LineFormat[Any]]], mirror.fromProduct)

  private def product[A](fields: List[LineFormat[Any]], build: Product => A): LineFormat[A] = new:
    def words(a: A): List[String] =
      a.asInstanceOf[Product].productIterator.toList.zip(fields).flatMap((value, field) => field.words(value))

    def read(words: List[String]): Option[(A, List[String])] =
      fields
        .foldLeft(Option((Vector.empty[Any], words))): (sofar, field) =>
          sofar.flatMap((values, rest) => field.read(rest).map((value, after) => (values :+ value, after)))
        .map((values, rest) => (build(Tuple.fromArray(values.map(_.asInstanceOf[Object]).toArray)), rest))
