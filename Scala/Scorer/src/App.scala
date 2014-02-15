import fs.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.CharBuffer

object App {
	def main(args: Array[String]) {
		println("app is begining--------")
				
		//block
		//while(true){}
		//test
		/*val fs:File = new File()
		fs.readBigFile("/Users/miloxia/Documents/void/AV/IPTD - 5889.rmvb", 0, (data:ByteBuffer) => { //size: 1.3G
			println("test3 count "+ data.limit)
		},(e:Throwable) => {
			println("error")
			e.printStackTrace()
		})*/
		val utf8 = Charset.forName("UTF-8");
	    val cb = CharBuffer.allocate(100);
		cb.put("hello");
		System.out.println("position "+cb.position()+" limit "+cb.limit());
		cb.flip();
		val g = ByteBuffer.allocate(100);
		g.asCharBuffer().put(cb);
		System.out.println("position "+g.position()+" limit "+g.limit());
		System.out.println(new String(g.array()));

	}
}