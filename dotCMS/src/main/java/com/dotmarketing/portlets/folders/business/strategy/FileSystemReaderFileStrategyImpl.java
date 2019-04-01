package com.dotmarketing.portlets.folders.business.strategy;

import com.liferay.util.StringPool;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class FileSystemReaderFileStrategyImpl implements ReaderFileStrategy {



    @Override
    public boolean test(final String file) {
        return null != file && file.toLowerCase().startsWith(FILE_SYSTEM_PREFIX);
    }

    @Override
    public Reader apply(final String file) throws IOException {
        return new FileReader(file.replace(FILE_SYSTEM_PREFIX, StringPool.BLANK));
    }

    @Override
    public Source source() {
        return Source.FILE_SYSTEM;
    }
}
