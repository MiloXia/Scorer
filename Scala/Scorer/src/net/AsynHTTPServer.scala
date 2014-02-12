package net

import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.AsynchronousSocketChannel
import java.nio.ByteBuffer
import java.util.concurrent.Future
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class AsynHTTPServer(tenThreadGroup:AsynchronousChannelGroup, port:Int) {
    private val server:AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open(tenThreadGroup).bind(new InetSocketAddress("localhost",port), 100)
    //private var f:Future[Integer] = null
    def createServer(hanlder:(HTTPRequest, HTTPResponse) => Unit) = {
        asynAccept(hanlder)
        this.server
    }
    
	private def asynAccept(hanlder:(HTTPRequest, HTTPResponse) => Unit) {
	    if (this.server.isOpen()) {
			server.accept(null, new CompletionHandler[AsynchronousSocketChannel, Any]() {

				@Override
				def completed(worker:AsynchronousSocketChannel, attachment:Any) {
			        try {
			            // read a message from the client, timeout after 10 seconds
			        	//asynRead2(worker);
			        	asynRead(worker, hanlder);
			        } catch {
			            case e:Exception => e.printStackTrace()
			            //println("Client didn't respond in time")
			        } finally {
			        	asynAccept(hanlder) //递归导致block...除非关闭
			        }
					
				}

				@Override
				def failed(exc:Throwable, attachment:Any) {
					//System.out.println("Client didn't respond in time");
					exc.printStackTrace()
					asynAccept(hanlder)
				}
			});

		}
	}
	
	private def asynRead(socket:AsynchronousSocketChannel, hanlder:(HTTPRequest, HTTPResponse) => Unit) { //callback
    	if (socket.isOpen()) {  
    		val readBuffer:ByteBuffer = ByteBuffer.allocate(1024)//太小会造成截断
            if (!readBuffer.hasRemaining()) {  
                //扩容
            }
            socket.read(readBuffer, null, new CompletionHandler[Integer, Any]() {
				@Override
				def completed(result:Integer, attachment:Any) {
				    /*if(result < 0) {
				        socket.close()
				        return
				    }*/
					if(result > 0) {
						println("Message received from client: \n" + new String(readBuffer.array()))
					    hanlder(new HTTPRequest(readBuffer), new HTTPResponse(socket)) //核心
					} else {
						asynRead(socket, hanlder)// 等待socket下一次可读 除非socket关闭了
					}
				}

				@Override
				def failed(exc:Throwable, attachment:Any) {
					exc.printStackTrace();
					println("error")
					server.close()
				}
            	
            });  
        } else {  
            throw new IllegalStateException(  
                    "Session Or Channel has been closed");  
        }  

    }
}
//test
object AsynHTTPServer {
    def main(args: Array[String]) {
        val http = new AsynHTTPServer(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()), 8888);
        println("server sart...")
        http.createServer((req:HTTPRequest, res:HTTPResponse) => {
            println("welcome")
            res.write("welcome");
            //res.end()
        })
    }
}