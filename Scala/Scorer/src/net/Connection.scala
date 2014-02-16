package net

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Future
import java.nio.channels.FileChannel
import java.nio.channels.Channels

class Connection(val socket:AsynchronousSocketChannel) {
    //需要为每个链接关联一个写入队,减轻group的压力
    private val queue = new ConcurrentLinkedQueue[ByteBuffer]();
    protected val readBuffer = ByteBuffer.allocate(1024)
    
    def getQueue() = this.queue
    def getReadBuffer() = this.readBuffer
    
    def start(handler:(HTTPRequest, HTTPResponse) => Unit) {
        asynRead(handler)
    }
    
    protected def asynRead(handler:(HTTPRequest, HTTPResponse) => Unit) { //callback
        if (this.socket.isOpen) {
            //val readBuffer = ByteBuffer.allocate(1024)//太小会造成截断
//            if (!readBuffer.hasRemaining) {
//                //TODO 扩容
//            }
            readBuffer.clear()
            socket.read(readBuffer, this, new CompletionHandler[Integer, Connection]() {
                @Override
                def completed(result:Integer, connection:Connection) {
                    if(result < 0) {
                        connection.close()
                        return
                    }
                    try {
                        if(result > 0) { //转入应用层
                            println("Message received from client: \n" + new String(readBuffer.array()))
                             /* decode出来的消息的派发给业务处理器工作最好交给一个线程池来处理，避免阻塞group绑定的线程池。
                              * 在Netty中分boss和worker线程池
                              */
                            handler(new HTTPRequest(connection), new HTTPResponse(connection)) //核心
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
                def failed(exc:Throwable, connection:Connection) {
                    exc.printStackTrace();
                    println("error")
                    connection.close()
                }
                
            });
        } else {
            throw new IllegalStateException("Channel has been closed");
        }
    }
    /*
     * 对一个队列上锁, 这时别的线程访问队列需要等待
     * 接着, 当前线程获取是否为空 以及添加内容到队列里, 然后释放锁
     * 然后如果为空的话 去执行写入操作, 这时别的线程获得锁
     * 获取是否为空, 并添加内容
     * 在前面线程写入完成之前都是不会调用 写入操作,因为 返回肯定不是为空
     * 所以第二个线程只是添加内容 而不实际写入
     * 真正在做写操作的 可能就几个线程
     * 这样 就减少了实际做写操作的线程数量, 从而减轻线程池的开销
     */
    def write(byteBuffer:ByteBuffer, handler:(Connection)=>Unit) {
        var canWrite = false
        queue.synchronized {
            canWrite = queue.isEmpty()
            queue.offer(byteBuffer)
        }
        if (canWrite) {
            asynWrite(byteBuffer, handler)
        }
    }
    
    def asynWrite(byteBuffer:ByteBuffer, handler:(Connection)=>Unit) {
        if(this.socket.isOpen) {//异步写入不支持发散聚集
            byteBuffer.flip()
            this.socket.write(byteBuffer, this, new CompletionHandler[Integer, Connection]() {
                @Override
                def completed(result:Integer, connection:Connection) {
                    var byteBuffer:ByteBuffer = null
                    val queue = connection.getQueue()
                    queue.synchronized {
                        //println("count "+result);
                        byteBuffer = queue.peek()
                        if(byteBuffer == null || !byteBuffer.hasRemaining) {
                            queue.remove()
                            handler(connection)// 对接应用层
                            byteBuffer = queue.peek() //保持最新的待写入元素
                        }
                    }
                    if(byteBuffer != null) {
                        try {
                            connection.asynWrite(byteBuffer, handler)
                        } catch {
                            //TODO handler exception
                            case e:Exception => println("connection has been closed")
                        }
                    }
                }

                @Override
                def failed(exc:Throwable, connection:Connection) {
                    exc.printStackTrace()
                    connection.close();
                }    
            })
        } else {  
          throw new IllegalStateException("Channel has been closed")
        }
    }
    
    private def asynWrite2(byteBuffer:ByteBuffer) {
        byteBuffer.flip()
       val f:Future[Integer] = this.socket.write(byteBuffer)
       val count:Integer = f.get()
       println("count "+count)
       this.socket.close() //TODO keep-alive
    }

    def pip(channel:FileChannel) {
        val output = Channels.newOutputStream(this.socket)
        val outChannel = Channels.newChannel(output)
        channel.transferTo(0, channel.size(), outChannel)
    }

    def close() {
        this.socket.close()
    }
}