这是一个用Scala编写的像Node.js一样的异步高性能应用服务器
======================================
你可以将其看成是JVM上的Node.js
---------------------------

### 你可以使用Scala或者Java语言来编写服务器端应用（java版本还在开发中，：））
#### 使用方式
```
//test
object AsynHTTPServer {
    def main(args: Array[String]) {
        val http = new AsynHTTPServer(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()))
        
        http.createServer((req:HTTPRequest, res:HTTPResponse) => {
            res.setHeader("Content-Type", "text/html")
            res.write("<h1>welcome</h1>");
            res.end()
        }).listen(8888)
    }
}

$ scala net.AsynHTTPServer
```
yes, yes, 你发现了，和Node长的还真神似，（这里有个小失误，等我全部都写完时，发现没有地方设置404,或者200，我把这么重要的事情给忘了，括弧笑，后续还会再补上，保证添加完整的HTTP Status Code）<br/>
<br/>
当然你还可以用pip()来返回静态文件
```
http.createServer((req:HTTPRequest, res:HTTPResponse) => {
    res.setHeader("Content-Type", "text/html")
    val fis:FileInputStream = new FileInputStream("/Users/miloxia/Desktop/hello.html")
    res.pip(fis.getChannel)
}).listen(8888)

```
### 关于pip()
pip()使用的是NoblockingIO的trainsTo(), 不在将文件从内核空间移至用户空间再写入socket，而是直接通过通道间传输，非常高效，但是目前pip()还不是异步的，后续版本将会提供异步版本，因为目前还没有单独为应用层提供worker线程池，(如Netty那样一个Boss线程池和一个worker线程池，目前我通通都是用的Boss，也就是上面代码中的ChannelGroup)<br/>

## 实现机制

### java AIO + Scala
异步特性全部来自java 异步IO，底层是Java，Scala将其封装成接口，后续在考虑伸缩性中会加入Scala Actor
### Proactor模式
传统的非堵塞NIO框架都采用Reactor模式，Reacot负责事件的注册、select、事件的派发；相应地，异步IO有个Proactor模式，Proactor负责 CompletionHandler的派发，查看一个典型的IO写操作的流程来看两者的区别：<br/>

Reactor:  send(msg) -> 消息队列是否为空，如果为空  -> 向Reactor注册OP_WRITE，然后返回 -> Reactor select -> 触发Writable，通知用户线程去处理 ->先注销Writable(很多人遇到的cpu 100%的问题就在于没有注销）,处理Writeable，如果没有完全写入，继续注册OP_WRITE。注意到，写入的工作还是用户线程在处理。<br/>
Proactor: send(msg) -> 消息队列是否为空，如果为空,发起read异步调用，并注册CompletionHandler，然后返回。 -> 操作系统负责将你的消息写入，并返回结果（写入的字节数）给Proactor -> Proactor派发CompletionHandler。可见，写入的工作是操作系统在处理，无需用户线程参与。事实上在aio的API 中,AsynchronousChannelGroup就扮演了Proactor的角色。<br/>

## TCP应用
### 如果要山寨那就要山寨的像点：
```
//test
object AsynTCPServer {
    def main(args: Array[String]) {
        val http = new AsynTCPServer(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()));

        http.createServer((socket:AsynSocket) => {
            socket.read((cunk:ByteBuffer) => {
                println(new String(cunk.array()))
                socket.write("hello")
            })
        }).listen(8888)
        println("server sart...")
    }
}

$ scala net.AsynTCPServer
```
为什么不是socket.on("data", callback), 很明星，还没有完成事件机制，不过把socket.read当成事件也是没问题的，就像jquery的$('#btn').click()一样，不过你放心，read是异步的，而且事件允许注册多个handler，对于某些事件，这显然不太好，正是因为这种方式会带来很大的复杂度，所以并没有一开始就实现事件机制，也去除了on的语义，事件和异步本身就是一体的，只不过哪些异步该做成事件，这很重要<br/>
### 如何减轻写入压力
<br />
看代码你会发现AsynSocket继承自Connection类，它代表一个连接，每个链接都关联一个写入队列，所有异步调用都会往队列里添加数据，但是真正执行写入的则只有几个（减轻Group的压力）。<br/>
在response的写入时，遵循Nagle算法, 会先将写入内容缓存起来，最后一次性写入(一次性写入时，调用connection的写入方法)，by the way write也是异步的。
<br />
### fs文件系统模块
目前只实现核心的异步读写，以及大文件读写（GB，和TB都没有问题）
```
val fs:File = new File()
fs.readBigFile("/Users/miloxia/Documents/video/Jobs.rmvb", 0, (data:ByteBuffer) => { //size: 1.3G
	println("test3 count "+ data.limit)
},(e:Throwable) => {
	println("error")
	e.printStackTrace()
})
```

1.3G 轻轻松松，因为使用了NIO的内存映射缓冲区<br/>

## TODO List

1. 完善HTTP Status Code
2. 完善HTTPRequest & HTTPResponse
3. 为应用层添加Worket 线程池 
4. 实现环形缓冲区解决TCP粘包和拆包问题
