package perfectcuboid

import cats.effect.IO
import fs2.io.file.Files
import scala.jdk.CollectionConverters.*

class HuntEventsTest extends munit.CatsEffectSuite:

  test("a flight recording of a hunt holds each block it searched and each checkpoint it wrote"):
    Files[IO].tempDirectory.use: dir =>
      val results = dir / "hunt.txt"
      val recordingFile = (dir / "hunt.jfr").toNioPath
      val recording = jdk.jfr.Recording()
      for
        _ <- IO:
          recording.enable("perfectcuboid.Block"): Unit
          recording.enable("perfectcuboid.Checkpoint"): Unit
          recording.start()
        _ <- Hunt.run(Quarry.eulerBricks, results, 5000, blockSize = 1000, cores = 2)
        events <- IO.blocking:
          recording.stop(): Unit
          recording.dump(recordingFile)
          recording.close()
          jdk.jfr.consumer.RecordingFile.readAllEvents(recordingFile).asScala.toList
      yield
        def named(name: String) = events.filter(_.getEventType.getName == name)
        val blocks = named("perfectcuboid.Block").sortBy(_.getLong("first"))
        val checkpoints = named("perfectcuboid.Checkpoint")
        assertEquals(
          blocks.map(e => (e.getLong("first"), e.getLong("last"))),
          (0 until 5).toList.map(k => (1000L * k + 1, 1000L * k + 1000))
        )
        assertEquals(blocks.map(_.getInt("finds")).sum, FastSearch.eulerBricksBetween(1, 5000).size)
        assertEquals(checkpoints.map(_.getLong("searchedUpTo")).sorted, List(1000L, 2000L, 3000L, 4000L, 5000L))
