package net

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.nio.charset.Charset
import java.nio.channels.GatheringByteChannel
import java.util.regex.Pattern
import java.util.concurrent.Future
import java.nio.channels.FileChannel

class HTTPResponse(connection:Connection) {
    //Nagle算法 会先将写入内容缓存起来，最后一次性写入
    private val dynamicHeader = CharBuffer.allocate(512)
    private val headerBuffer = CharBuffer.allocate (1024)
    private val cacheBuffer = CharBuffer.allocate (1024)
    private val resultBuffer = ByteBuffer.allocate (1024)
    //def static header
    private val  LINE_SEP:String = "\r\n";
    private val  HTTP_HDR:String =
        "HTTP/1.0 200 OK" + LINE_SEP + //暂时不支持404等 括弧笑
        "Server: Ronsoft Sample Server" + LINE_SEP;
    private val staticHdr:ByteBuffer = ByteBuffer.wrap (HTTP_HDR.getBytes());
    //end
    private val utf8:Charset = Charset.forName ("UTF-8");
    private val space:Pattern = Pattern.compile ("\\s+");
    
    def write(data:String) {
        cacheBuffer.put(data)
    }

    private def asynWrite(byteBuffer:ByteBuffer) {
        println( new String(byteBuffer.array(), "utf-8"))
            connection.write(byteBuffer, (conn) => {
                conn.close() //TODO keep-alive
            })
    }

    def end() {
        setTotalHeader(cacheBuffer.position)
        this.resultBuffer.clear()
//      println("postion "+resultBuffer.position()+" limit "+resultBuffer.limit())
//      println("postion "+headerBuffer.position()+" limit "+headerBuffer.limit())
//      println("postion "+cacheBuffer.position()+" limit "+cacheBuffer.limit())
        this.headerBuffer.flip()
        this.cacheBuffer.flip()
        this.resultBuffer.put(utf8.encode(this.headerBuffer))
        this.resultBuffer.put(utf8.encode(this.cacheBuffer))
        asynWrite(resultBuffer)
    }
    
    def end(data:String) {
        this.write(data)
        end()
    }
    
    private def setTotalHeader(length:Int) {
        staticHdr.rewind()
        this.headerBuffer.clear()
        this.headerBuffer.put(utf8.decode(staticHdr))
        //dynamic header
        this.headerBuffer.put ("Content-Length: ").put(length.toString)
        this.headerBuffer.put (LINE_SEP)
        this.dynamicHeader.flip()
        this.headerBuffer.put (this.dynamicHeader)
        this.headerBuffer.put (LINE_SEP)
        this.headerBuffer.put (LINE_SEP)
    }
    
    def setHeader(key:String, value:String) {
        this.dynamicHeader.put(key).put(": ").put(value); 
    }
    
    def setHeader(option:Map[String, String]) {
        //TODO set option to response header
    }
    
    def pip(channel:FileChannel) {
        this.connection.pip(channel)
        this.connection.close()
    }
    
}