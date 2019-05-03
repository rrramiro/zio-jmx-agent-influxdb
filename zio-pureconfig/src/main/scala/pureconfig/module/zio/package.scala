package pureconfig.module

import java.io.OutputStream
import java.nio.file.Path

import scala.reflect.ClassTag

import com.typesafe.config.ConfigRenderOptions
import pureconfig._
import pureconfig.error.ConfigReaderException
import _root_.zio._

package object zio {

  private def configToIO[A: ClassTag](
      getConfig: => ConfigReader.Result[A]
  ): Task[A] =
    IO.fromEither(getConfig).mapError(ConfigReaderException[A])

  implicit class ZioConfigSource(cs: ConfigSource) {
    def loadIO[A](
        implicit reader: Derivation[ConfigReader[A]],
        ct: ClassTag[A]
    ): Task[A] = configToIO(cs.load[A])
  }

  def saveConfigAsPropertyFileIO[A](
      conf: A,
      outputPath: Path,
      overrideOutputPath: Boolean = false,
      options: ConfigRenderOptions = ConfigRenderOptions.defaults()
  )(implicit writer: Derivation[ConfigWriter[A]]): Task[Unit] = IO.effect {
    pureconfig
      .saveConfigAsPropertyFile(conf, outputPath, overrideOutputPath, options)
  }

  def saveConfigToStreamF[A](
      conf: A,
      outputStream: OutputStream,
      options: ConfigRenderOptions = ConfigRenderOptions.defaults()
  )(implicit writer: Derivation[ConfigWriter[A]]): Task[Unit] = IO.effect {
    pureconfig.saveConfigToStream(conf, outputStream, options)
  }

}
