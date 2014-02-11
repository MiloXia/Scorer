package fs

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.MappedByteBuffer
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class File {
    
	def write(filename:String, content:String) = {
	  
	}
	/**
	 * readFile/1
	 * 异步读取文件内容
	 * 不支持大文件，返回回调函数参数为文件的字节序列
	 */
	def readFile(filename:String, success:(ByteBuffer) => Unit, error:Throwable => Unit) = {
		val path:Path = Paths.get(filename);
        val fileChannel = AsynchronousFileChannel.open(path, 
                StandardOpenOption.READ/*, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE*/);
        val buffer:ByteBuffer = ByteBuffer.allocate(Files.size(path).toInt) //不支持大文件,太大肯定会溢出(需要调节堆栈)
        fileChannel.read(buffer, 0, null, new CompletionHandler[Integer, Any](){
            @Override
            def completed(result:Integer, attachment:Any) {
                //System.out.println("Read operation completed, file contents is: " + new String(buffer.array()))
                success(buffer)
                clearUp(fileChannel)
            }
            @Override
            def failed(e:Throwable, attachment:Any) {
                error(e)
                clearUp(fileChannel)
            }
            
        });
	}
	/**
	 * readFile/2
	 * 异步读取文件内容
	 * 不支持大文件（分段读写），返回回调函数参数为文件的字节数
	 */
	def readFile(filename:String, begin:Int, buffer:ByteBuffer, success:(Int) => Unit, error:Throwable => Unit) = {
		val path:Path = Paths.get(filename);
        val fileChannel = AsynchronousFileChannel.open(path, 
                StandardOpenOption.READ/*, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE*/);
        fileChannel.read(buffer, begin, null, new CompletionHandler[Integer, Any](){
            @Override
            def completed(result:Integer, attachment:Any) {
                //System.out.println("Read operation completed, file contents is: " + new String(buffer.array()))
                success(result.intValue)
                clearUp(fileChannel)
            }
            @Override
            def failed(e:Throwable, attachment:Any) {
                error(e)
                clearUp(fileChannel)
            }
            
        });
	}
	/**
	 * readBigFile
	 * 异步读取文件内容
	 * 支持大文件，返回回调函数参数为文件的字节
	 */
	def readBigFile(filename:String, timeout:Int, success:(ByteBuffer) => Unit, error:Throwable => Unit) {
	    val service:ExecutorService = Executors.newSingleThreadExecutor();
	    val future:Future[MappedByteBuffer] = service.submit(new Callable[MappedByteBuffer]() {
	        @Override
			def  call():MappedByteBuffer = {
			    var fis:FileInputStream = null
				var fc:FileChannel = null
				try {
			        fis = new FileInputStream (filename);
					fc = fis.getChannel();
			    	val fileData:MappedByteBuffer = fc.map(MapMode.READ_ONLY, 0, fc.size()); //内存映射  
			    	//success(fileData)
			    	fileData
				} catch {
				    case e:Exception => error(e); null
			    } finally {
			        fc.close()
			        fis.close()
			    }
			}
		})
		if(timeout != 0) {
			fianly(service)
		} else {
		    while(!future.isDone()) {
	    		fianly(service)
	        }
		}
	    def fianly(service:ExecutorService) {
	        try {
                success(future.get())
            } catch {
              case e:Exception => error(e)
            } finally {
            	service.shutdown(); //否则不会关闭
            }		
	    }
	}
	/**
	 * readBigFile
	 * 同步读取文件内容
	 * 支持大文件，返回回调函数参数为文件的字节
	 */
	def readBigFile(filename:String, success:(ByteBuffer) => Unit, error:Throwable => Unit) {
	    var fis:FileInputStream = null
		var fc:FileChannel = null
		try {
	        fis = new FileInputStream (filename);
			fc = fis.getChannel();
	    	val fileData:MappedByteBuffer = fc.map(MapMode.READ_ONLY, 0, fc.size()); //内存映射  
	    	success(fileData)
		} catch {
		    case e:Exception => error(e)
	    } finally {
	        fc.close()
	        fis.close()
	    }
	}

	
	private def clearUp(fileChannel:AsynchronousFileChannel) {
		try {
			fileChannel.close()
		} catch {
          case e:IOException => e.printStackTrace()
        }
    }
}

//test
object File {
	def main(args: Array[String]) {
		println("app is begining--------")
		val filename = "/Users/miloxia/Desktop/javac.rb"
		val fs:File = new File()
		for(i <- 1 to 2) { //异步调用，完成顺序不一定
			fs.readFile(filename, (data:ByteBuffer) => {
				println("test1 \n "+new String(data.array))
			},(e:Throwable) => {
				println("error")
				e.printStackTrace()
			})
			
			fs.readFile(filename, (data:ByteBuffer) => {
				println("test2 count "+ data.limit)
			},(e:Throwable) => {
				println("error")
				e.printStackTrace()
			})
		}
		//test
		fs.readBigFile("/Users/miloxia/Documents/void/AV/IPTD - 5889.rmvb", 100,  (data:ByteBuffer) => { //size: 1.3G
			println("test3 count "+ data.limit)
		},(e:Throwable) => {
			println("error")
			e.printStackTrace()
		})
		//block
		while(true){}
	}
}