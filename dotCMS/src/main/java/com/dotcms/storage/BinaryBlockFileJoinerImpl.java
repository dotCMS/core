package com.dotcms.storage;

import com.dotcms.util.FileJoiner;
import com.dotcms.util.FileJoinerImpl;
import org.apache.commons.io.output.DeferredFileOutputStream;

import java.io.File;

/**
 * Join into the bytes into a memory until reached the max size, switch to a file
 * {@link BinaryBlockFileJoinerImpl#getBytes()} is not null when the file contents is on memory, otherwise is null and the content is into the file.
 * @author jsanca
 */
public class BinaryBlockFileJoinerImpl extends FileJoinerImpl implements FileJoiner {


    public BinaryBlockFileJoinerImpl(final File file, final int maxSize) {
        super(new DeferredFileOutputStream(maxSize, file));
    }

    public byte [] getBytes() {

        final DeferredFileOutputStream outputStream = (DeferredFileOutputStream)this.getOutputStream();
        return outputStream.isInMemory()?outputStream.getData():null;
   }

}
