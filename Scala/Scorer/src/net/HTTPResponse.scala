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

class HTTPResponse(socket:AsynchronousSocketChannel) {
    //Nagle算法 需要为每个链接关联一个写入队列
    private val queue:BlockingQueue[ByteBuffer] = new LinkedBlockingQueue(HTTPResponse.QUEUE_SIZE);
	
    def write(data:String) {
        val byteBuffer:ByteBuffer = ByteBuffer.wrap(data.getBytes());
	    //this.socket.write()
	    val canWrite = this.queue.isEmpty();
	    try {
	    	this.queue.put(byteBuffer); //may block
	    } catch {
	    	case e:Exception => e.printStackTrace()
	    }
	    if(canWrite) {
	        asynWrite(byteBuffer);
	    }
	}
    
    private def asynWrite(byteBuffer:ByteBuffer) {
	    if(this.socket.isOpen) {//异步写入不支持发散聚集
	        this.socket.write(warpHTTPBuffer(byteBuffer), null, new CompletionHandler[Integer, Any]() {
				@Override
				def completed(result:Integer, attachment:Any) {
				    //queue.synchronized {
				    println("count "+result);
				        if(!byteBuffer.hasRemaining) {
				            try {
				               queue.take() //may block
				            } catch {
				                case e:Exception => e.printStackTrace()
				            }
				            socket.close(); 
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
    private val  LINE_SEP:String = "\r\n";
	private val  HTTP_HDR:String =
		"HTTP/1.0 200 OK" + LINE_SEP +
		"Server: Ronsoft Sample Server" + LINE_SEP;

	private def staticHdr:ByteBuffer = ByteBuffer.wrap (HTTP_HDR.getBytes());

	private val utf8:Charset = Charset.forName ("UTF-8");
	private val space:Pattern = Pattern.compile ("\\s+");
    //private val cbtemp:CharBuffer = CharBuffer.allocate (1024);
	//private val dynHdr:ByteBuffer = ByteBuffer.allocate (1024);

	private def warpHTTPBuffer(data:ByteBuffer/*, contentType:String*/):ByteBuffer = {
		val cbtemp:CharBuffer = CharBuffer.allocate (1024);
		staticHdr.rewind();//类似filp limt不变 对warp的数组比较合适

		cbtemp.clear();
		//header
		cbtemp.put(utf8.decode(staticHdr))
		cbtemp.put ("Content-Length: " + data.limit());
		cbtemp.put (LINE_SEP);
		cbtemp.put ("Content-Type: ");
		cbtemp.put (/*contentType*/"text/plain");
		cbtemp.put (LINE_SEP);
		cbtemp.put (LINE_SEP);
		//body
		cbtemp.put(utf8.decode(data));
		cbtemp.flip();
		println(new String(utf8.encode(cbtemp).array(), "utf-8"))
		val result:ByteBuffer = utf8.encode(cbtemp);//CharBuffer --> ByteBuffer
		result.flip();
		result
	}

    
	def end() {
	    socket.close()
	}
	
}
object HTTPResponse {
    val QUEUE_SIZE:Int = 10
}