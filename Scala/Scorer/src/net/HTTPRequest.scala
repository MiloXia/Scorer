package net

import java.nio.ByteBuffer

class HTTPRequest(connection:Connection) {
    private val incomingMessage = connection.getReadBuffer
    
    def header() = {
        //TODO split the request header
    }

    def url() = {
        //TODO split the request url
    }

    def param = {
        //TODO get the query string
    }
}