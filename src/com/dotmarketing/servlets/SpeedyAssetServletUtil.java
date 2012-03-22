package com.dotmarketing.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * @see {@link http://programmaremobile.blogspot.com/2009/01/iphone-file-download-eng-ver.html}
 * @author Roger
 *
 */
public class SpeedyAssetServletUtil {
	
	protected static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
	protected static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.
	
	/**
     * Returns a substring of the given string value from the given begin index to the given end
     * index as a long. If the substring is empty, then -1 will be returned
     * @param value The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }


	/**
	 * Parse the range values of the given string
	 * @param rangeHeader String containing the range format "bytes=n-n,n-n,n-n...".
	 * @param dataLen length of the byte range
	 * @return
	 */
	protected static ArrayList<ByteRange> parseRange(String rangeHeader, int length){
        ArrayList<ByteRange> ranges = null;
        if (rangeHeader != null && rangeHeader.startsWith("bytes")){            
            ranges = new ArrayList<ByteRange>(8);
        for (String part : rangeHeader.substring(6).split(",")) {
            // Assuming a file with length of 100, the following examples returns bytes at:
            // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
            long start = sublong(part, 0, part.indexOf("-"));
            long end = sublong(part, part.indexOf("-") + 1, part.length());

            if (start == -1) {
                start = length - end;
                end = length - 1;
            } else if (end == -1 || end > length - 1) {
                end = length - 1;
            }
            // Add range.
            ranges.add(new ByteRange(start, end, length));
         }
        }

        return ranges;
    } 
	
    /**
     * Copy the given byte range of the given input to the given output.
     * @param input The input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */
	protected static void copy(RandomAccessFile input, OutputStream output, long start, long length)throws IOException{
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		int read;

		if (input.length() == length) {
			// Write full range.
			while ((read = input.read(buffer)) > 0) {
				output.write(buffer, 0, read);
			}
		} else {
			// Write partial range.
			input.seek(start);
			long toRead = length;

			while ((read = input.read(buffer)) > 0) {
				if ((toRead -= read) > 0) {
					output.write(buffer, 0, read);
				} else {
					output.write(buffer, 0, (int) toRead + read);
					break;
				}
			}
		}
	}

    
   protected static class ByteRange {
	   long start;
       long end;
       long length;
       long total;

       /**
        * Construct a byte range.
        * @param start Start of the byte range.
        * @param end End of the byte range.
        * @param total Total length of the byte source.
        */
       public ByteRange(long start, long end, long total) {
           this.start = start;
           this.end = end;
           this.length = end - start + 1;
           this.total = total;
       }
       
       @Override
       public boolean equals(Object obj){
    	   if(obj==this)  
    		  return true;
    	
    	   if(!(obj instanceof ByteRange))
    		   return false;
    	   
    	   ByteRange br  = (ByteRange)obj;
    	   return (br.start==this.start &&
    			   br.end==this.end &&
    			   br.length==this.length &&
    			   br.total==this.total);
       }
       
       @Override 
       public int hashCode(){
    	   int result = 17;
    	   result = 31 * result +(int) (start ^ (start >>> 32));
    	   result = 31 * result +(int) (end ^ (end >>> 32));
    	   result = 31 * result +(int) (length ^ (length >>> 32));
    	   result = 31 * result +(int) (total ^ (total >>> 32));
    	   return result;
       }

    } 



}
