package net

import java.nio.channels.AsynchronousSocketChannel

class HTTPResponse(socket:AsynchronousSocketChannel) {
	def write() {
	    //this.socket.write()
	}
	def end() {
	    socket.close()
	}
}