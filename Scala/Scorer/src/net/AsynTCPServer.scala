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

class AsynTCPServer(tenThreadGroup:AsynchronousChannelGroup) {
    
    private val server:AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open(tenThreadGroup)
    private var hanlder:Function1[AsynSocket, Unit] = null

    def listen(port:Int) {
        server.bind(new InetSocketAddress("localhost",port), 100)
        asynAccept(this.hanlder)
    }

    def createServer(hanlder:(AsynSocket) => Unit) = {
        this.hanlder = hanlder
        this
    }
    
    private def asynAccept(hanlder:(AsynSocket) => Unit) {
        if (this.server.isOpen) {
            server.accept(null, new CompletionHandler[AsynchronousSocketChannel, Any]() {

                @Override
                def completed(socket:AsynchronousSocketChannel, attachment:Any) {
                    try {
                        // read a message from the client, timeout after 10 seconds
                        println("Accept connection from " + socket.getRemoteAddress())
                        hanlder(new AsynSocket(socket))
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
object AsynTCPServer {
    def main(args: Array[String]) {
        val http = new AsynTCPServer(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()));
        println("server sart...")
        http.createServer((socket:AsynSocket) => {
            socket.read((cunk:ByteBuffer) => {
                println(new String(cunk.array()))
                socket.write("hello")
            })
        }).listen(8888)
    }
}