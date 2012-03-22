package com.dotmarketing.filters.FixCmis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Logger;

public class FixCmisResponseStream extends ServletOutputStream {
	protected ByteArrayOutputStream baos = null;
	protected boolean closed = false;
	protected HttpServletResponse response = null;
	protected ServletOutputStream output = null;
	int length =0;
	
	public FixCmisResponseStream(HttpServletResponse response) throws IOException {
		super();
		closed = false;
		this.response = response;
		this.output = response.getOutputStream();
		this.length=0;

	}

	public void close() throws IOException {
		if (closed) {
			throw new IOException("This output stream has already been closed");
		}




		response.addHeader("Content-Length", Integer.toString(this.length));
		output.flush();
		output.close();
		closed = true;
	}

	public void flush() throws IOException {
		if (closed) {
			throw new IOException("Cannot flush a closed output stream");
		}
		output.flush();
	}

	public void write(int b) throws IOException {
		if (closed) {
			throw new IOException("Cannot write to a closed output stream");
		}
		output.write((byte) b);
		length = length+1;
	}

	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	public void write(byte b[], int off, int len) throws IOException {
		Logger.debug(this, "writing bytes to gzip stream gzip");
		if (closed) {
			throw new IOException("Cannot write to a closed output stream");
		}
		
		boolean haveSemiColon = false;
		
		for(int i =0;i<len;i++){
			if(!(b[i] ==  32 && haveSemiColon)){

				write(b[i]);
			}
			if(b[i] == 59){
				haveSemiColon = true;
			}
			else{
				haveSemiColon = false;
			}
		}
		
		
		//output.write(b, off, len);
	}

	public boolean closed() {
		return (this.closed);
	}


}
