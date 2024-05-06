package com.ettrema.httpclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 *
 * @author mcevoyb
 */
public class MyByteArrayOutputStream extends ByteArrayOutputStream {

    public MyByteArrayOutputStream() {
        super();
    }

    public void readFully( InputStream in ) throws IOException {
        byte[] arr = new byte[1024];
        int i = in.read( arr );
        while( i >= 0 ) {
            write( arr, 0, i );
            i = in.read( arr );
        }
    }

    public InputStream asIn() {
        ByteArrayInputStream in = new ByteArrayInputStream( this.toByteArray() );
        return in;
    }
}
