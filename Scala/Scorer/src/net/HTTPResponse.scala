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

class HTTPResponse(socket:AsynchronousSocketChannel) {
    //Nagle算法 会先将写入内容缓存起来，最后一次性写入
    private val dynamicHeader = CharBuffer.allocate(512)
    private val headerBuffer = CharBuffer.allocate (1024)
    private val cacheBuffer = CharBuffer.allocate (1024)
    private val resultBuffer = ByteBuffer.allocate (1024)
    //def static header
    private val  LINE_SEP:String = "\r\n";
	private val  HTTP_HDR:String =
		"HTTP/1.0 200 OK" + LINE_SEP +
		"Server: Ronsoft Sample Server" + LINE_SEP;
	private val staticHdr:ByteBuffer = ByteBuffer.wrap (HTTP_HDR.getBytes());
	//end
	private val utf8:Charset = Charset.forName ("UTF-8");
	private val space:Pattern = Pattern.compile ("\\s+");
	
    def write(data:String) {
        //val byteBuffer:ByteBuffer = ByteBuffer.wrap(data.getBytes());
	    //this.socket.write()
	    /*val canWrite = this.queue.isEmpty();
	    try {
	    	this.queue.put(byteBuffer); //may block
	    } catch {
	    	case e:Exception => e.printStackTrace()
	    }
	    if(canWrite) {
	        asynWrite(byteBuffer);
	    }*/
        cacheBuffer.put(data)
	}
    
    private def asynWrite(byteBuffer:ByteBuffer) {
        println( new String(byteBuffer.array(), "utf-8"))
        if(this.socket.isOpen) {//异步写入不支持发散聚集
            byteBuffer.flip()
	        this.socket.write(byteBuffer, null, new CompletionHandler[Integer, Any]() {
				@Override
				def completed(result:Integer, attachment:Any) {
				    //queue.synchronized {
				    println("count "+result);
				        if(!byteBuffer.hasRemaining) {
				            /*try {
				               queue.take() //may block
				            } catch {
				                case e:Exception => e.printStackTrace()
				            }*/
				            socket.close();//TODO keep-alive
				        } else {
				            asynWrite(byteBuffer)
				        }
				    //}
				}

				@Override
				def failed(exc:Throwable, attachment:Any) {
					exc.printStackTrace();
					socket.close(); //server.close();
				}	
            })
	       
	    }
	}
    
    private def asynWrite2(byteBuffer:ByteBuffer) {
        byteBuffer.flip()
       val f:Future[Integer] = this.socket.write(byteBuffer)
       val count:Integer = f.get()
       println("count "+count)
       this.socket.close() //TODO keep-alive
    }
    
//  private val cbtemp:CharBuffer = CharBuffer.allocate (1024);
//  private val dynHdr:ByteBuffer = ByteBuffer.allocate (1024);
//  no use
//	private def warpHTTPBuffer(data:ByteBuffer/*, contentType:String*/):ByteBuffer = {
//		val cbtemp:CharBuffer = CharBuffer.allocate (1024);
//		staticHdr.rewind();//类似filp limt不变 对warp的数组比较合适
//
//		cbtemp.clear();
//		//header
//		cbtemp.put(utf8.decode(staticHdr))
//		cbtemp.put ("Content-Length: " + data.limit());
//		cbtemp.put (LINE_SEP);
//		cbtemp.put ("Content-Type: ");
//		cbtemp.put (/*contentType*/"text/plain");
//		cbtemp.put (LINE_SEP);
//		cbtemp.put (LINE_SEP);
//		//body
//		cbtemp.put(utf8.decode(data));
//		cbtemp.flip();
//		println(new String(utf8.encode(cbtemp).array(), "utf-8"))
//		val result:ByteBuffer = utf8.encode(cbtemp);//CharBuffer --> ByteBuffer
//		result.flip();
//		result
//	}

    
	def end() {
	    setTotalHeader(cacheBuffer.position)
	    this.resultBuffer.clear()
//	    println("postion "+resultBuffer.position()+" limit "+resultBuffer.limit())
//	    println("postion "+headerBuffer.position()+" limit "+headerBuffer.limit())
//	    println("postion "+cacheBuffer.position()+" limit "+cacheBuffer.limit())
	    this.headerBuffer.flip()
	    this.cacheBuffer.flip()
	    this.resultBuffer.put(utf8.encode(this.headerBuffer))
	    this.resultBuffer.put(utf8.encode(this.cacheBuffer))
	    asynWrite(resultBuffer)
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
	    //TODO
	}
	
}