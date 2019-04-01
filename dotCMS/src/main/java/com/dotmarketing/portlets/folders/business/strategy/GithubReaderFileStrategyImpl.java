package com.dotmarketing.portlets.folders.business.strategy;

import java.io.IOException;
import java.io.Reader;

public class GithubReaderFileStrategyImpl implements ReaderFileStrategy {

    @Override
    public boolean test(final String file) {
        return null != file && file.toLowerCase().startsWith(GITHUB_SYSTEM_PREFIX);
    }

    @Override
    public Reader apply(final String file) throws IOException {

        return null; // todo: implement me
    }



    @Override
    public Source source() {
        return Source.GITHUB;
    }
}
