import fs.File
import java.nio.ByteBuffer

object App {
	def main(args: Array[String]) {
		println("app is begining--------")
				
		//block
		//while(true){}
		//test
		val fs:File = new File()
		fs.readBigFile("/Users/miloxia/Documents/void/AV/IPTD - 5889.rmvb", 0, (data:ByteBuffer) => { //size: 1.3G
			println("test3 count "+ data.limit)
		},(e:Throwable) => {
			println("error")
			e.printStackTrace()
		})

	}
}