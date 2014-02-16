package net

import java.nio.channels.AsynchronousSocketChannel
import java.nio.ByteBuffer
import java.nio.channels.CompletionHandler

class AsynSocket(socket:AsynchronousSocketChannel) extends Connection(socket) {

    private val writeBuffer = ByteBuffer.allocate(1024)
    def read(handler:(ByteBuffer) => Unit) {
        asynRead(handler)
    }
    
    def asynRead(handler:(ByteBuffer) => Unit) {
        if (this.socket.isOpen) {
            //val readBuffer = ByteBuffer.allocate(1024)//太小会造成截断
//            if (!readBuffer.hasRemaining) {
//                //TODO 扩容
//            }
            readBuffer.clear()
            socket.read(readBuffer, this, new CompletionHandler[Integer, AsynSocket]() {
                @Override
                def completed(result:Integer, connection:AsynSocket) {
                    if(result < 0) {
                        connection.close()
                        return
                    }
                    try {
                        if(result > 0) { //转入应用层
                            //println("Message received from client: \n" + new String(readBuffer.array()))
                            handler(readBuffer) //核心
                        }
                    } finally {
                        try {
                            connection.asynRead(handler)// 等待socket下一次可读 除非socket关闭了
                        } catch {
                            //TODO handler exception
                            case e:Exception => println("connection has been closed")
                        }
                    }
                }

                @Override
                def failed(exc:Throwable, connection:AsynSocket) {
                    exc.printStackTrace();
                    println("error")
                    connection.close()
                }
                
            });
        } else {
            throw new IllegalStateException("Channel has been closed");
        }
    }
    def write(data:String, handler:(Connection) => Unit) {
        this.writeBuffer.clear()
        this.writeBuffer.asCharBuffer().put(data)
        super.write(ByteBuffer.wrap(data.getBytes()), handler)
    }

    def write(data:String) {
        this.writeBuffer.clear()
        if(!this.writeBuffer.hasRemaining) {
            //TODO 扩容
        }
        this.writeBuffer.put(data.getBytes())
        super.write(writeBuffer, (conn:Connection) => {})
    }

}