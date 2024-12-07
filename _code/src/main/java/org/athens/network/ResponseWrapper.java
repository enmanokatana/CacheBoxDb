package org.athens.network;

import org.athens.keyvalue.KeyValueDB.Response;

import java.nio.channels.SocketChannel;

public class ResponseWrapper {
    private final Response response;
    private final SocketChannel channel;

    public ResponseWrapper(Response response, SocketChannel channel) {
        this.response = response;
        this.channel = channel;
    }

    public Response getResponse() {
        return response;
    }

    public SocketChannel getChannel() {
        return channel;
    }
}