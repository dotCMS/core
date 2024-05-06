package com.dotcms.api.client.push;

import com.dotcms.model.push.PushAnalysisResult;
import java.io.File;
import java.util.List;

/**
 * Service interface for performing push analysis on a local file or folder.
 */
public interface PushAnalysisService {

    /**
     * Analyzes a local file or folder and generates a list of push analysis results.
     *
     * @param localFileOrFolder the local file or folder to analyze
     * @param allowRemove       whether to allow removals
     * @param provider          the content fetcher used to retrieve content
     * @param comparator        the content comparator used to compare content
     * @return a list of push analysis results
     */
    <T> List<PushAnalysisResult<T>> analyze(File localFileOrFolder,
            boolean allowRemove,
            ContentFetcher<T> provider,
            ContentComparator<T> comparator);

}

