package com.gu.contentapi.firehose.kinesis

import com.twitter.scrooge.{ ThriftStruct, ThriftStructCodec }
import org.apache.thrift.protocol.TCompactProtocol
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream

import java.nio.ByteBuffer
import org.apache.thrift.transport.TIOStreamTransport

import scala.util.Try

trait ThriftDeserializer[T <: ThriftStruct] {

  val codec: ThriftStructCodec[T]

  def deserializeEvent(buffer: ByteBuffer): Try[T] = {
    Try {
      val bbis = new ByteBufferBackedInputStream(buffer)
      val transport = new TIOStreamTransport(bbis)
      val protocol = new TCompactProtocol(transport)
      codec.decode(protocol)
    }
  }
}
