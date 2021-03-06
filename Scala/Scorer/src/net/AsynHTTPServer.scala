package net

import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.AsynchronousSocketChannel
import java.nio.ByteBuffer
import java.util.concurrent.Future
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.io.FileInputStream
import java.nio.channels.FileChannel

class AsynHTTPServer(tenThreadGroup:AsynchronousChannelGroup) {
    
    private val server:AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open(tenThreadGroup)
    private var hanlder:Function2[HTTPRequest, HTTPResponse, Unit] = null

    def listen(port:Int) {
        server.bind(new InetSocketAddress("localhost",port), 100)
        asynAccept(this.hanlder)
    }

    def createServer(hanlder:(HTTPRequest, HTTPResponse) => Unit) = {
        this.hanlder = hanlder
        this
    }
    
    private def asynAccept(hanlder:(HTTPRequest, HTTPResponse) => Unit) {
        if (this.server.isOpen) {
            server.accept(null, new CompletionHandler[AsynchronousSocketChannel, Any]() {

                @Override
                def completed(socket:AsynchronousSocketChannel, attachment:Any) {
                    try {
                        // read a message from the client, timeout after 10 seconds
                        println("Accept connection from " + socket.getRemoteAddress())
                        val connection = new Connection(socket)
                        connection.start(hanlder)
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
}
//test
object AsynHTTPServer {
    def main(args: Array[String]) {
        val http = new AsynHTTPServer(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()));
        println("server sart...")
        http.createServer((req:HTTPRequest, res:HTTPResponse) => {
            res.setHeader("Content-Type", "text/html")
            res.write("<h1>welcome</h1>");
            res.end()
            //val fis:FileInputStream = new FileInputStream("/Users/miloxia/Desktop/hello.html")
            //val channel:FileChannel = fis.getChannel()
            //res.pip(channel)
        }).listen(8888)
    }
}