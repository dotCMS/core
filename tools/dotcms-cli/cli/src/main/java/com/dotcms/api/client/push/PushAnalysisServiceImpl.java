package com.dotcms.api.client.push;

import com.dotcms.api.client.MapperService;
import com.dotcms.cli.common.HiddenFileFilter;
import com.dotcms.model.push.PushAction;
import com.dotcms.model.push.PushAnalysisResult;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * This class provides an implementation of the PushAnalysisService interface. It analyzes local
 * files against server files to find updates, additions, and removals. The analysis results are
 * returned as a list of PushAnalysisResult objects.
 */
@DefaultBean
@Dependent
public class PushAnalysisServiceImpl implements PushAnalysisService {

    @Inject
    MapperService mapperService;

    @Inject
    Logger logger;

    /**
     * Analyzes the local files against server files to find updates, additions, and removals based
     * on the provided local files, server files, and content comparator. The analysis is performed
     * by comparing the content of the local files with the content of the server files using the
     * specified content comparator.
     *
     * @param localFile   the local file or directory to be analyzed
     * @param allowRemove whether to allow removals
     * @param provider    the content fetcher that provides the server files content
     * @param comparator  the content comparator used to compare the content of local and server
     *                    files
     * @return a list of push analysis results which include updates, additions, and removals found
     * during the analysis
     */
    @ActivateRequestContext
    public <T> List<PushAnalysisResult<T>> analyze(final File localFile,
            final boolean allowRemove,
            final ContentFetcher<T> provider,
            final ContentComparator<T> comparator) {

        List<File> localContents = readLocalContents(localFile);
        List<T> serverContents = provider.fetch();

        // Checking local files against server files to find updates and additions
        List<PushAnalysisResult<T>> results = new ArrayList<>(
                checkLocal(localContents, serverContents, comparator)
        );

        if (allowRemove) {

            // We don't need to check the server against local files if we are dealing with a single file
            if (localFile.isDirectory()) {
                // Checking server files against local files to find removals
                results.addAll(checkServerForRemovals(localContents, serverContents, comparator));
            }
        }

        return results;
    }

    /**
     * This method analyzes local files and server contents using a content comparator to determine
     * the appropriate actions to perform. It returns a list of PushAnalysisResult objects that
     * represent the analysis results.
     *
     * @param localFiles     a list of local files to be analyzed
     * @param serverContents a list of server contents to be compared against
     * @param comparator     a content comparator used to compare local content with server
     *                       contents
     * @return a list of PushAnalysisResult objects representing the analysis results
     */
    private <T> List<PushAnalysisResult<T>> checkLocal(List<File> localFiles,
            List<T> serverContents, ContentComparator<T> comparator) {

        List<PushAnalysisResult<T>> results = new ArrayList<>();

        for (File localFile : localFiles) {

            var localContent = map(localFile, comparator.type());

            var matchingServerContent = comparator.findMatchingServerContent(
                    localFile,
                    localContent,
                    serverContents
            );
            if (matchingServerContent.isPresent()) {

                var action = PushAction.NO_ACTION;
                if (!comparator.contentEquals(localContent, matchingServerContent.get())) {
                    logger.warn("Local file " + localFile + " has differences from server");
                    action = PushAction.UPDATE;
                }

                results.add(
                        PushAnalysisResult.<T>builder().
                                action(action).
                                localFile(localFile).
                                localContent(localContent).
                                serverContent(matchingServerContent).
                                build()
                );
            } else {
                results.add(
                        PushAnalysisResult.<T>builder().
                                action(PushAction.ADD).
                                localContent(localContent).
                                localFile(localFile).
                                build()
                );
            }
        }

        // Sort the results by the order they should be processed.
        //  This is useful because the order of the results is not guaranteed and only will be used
        //  if a custom comparator is provided.
        Comparator<T> orderComparator = comparator.getProcessingOrderComparator();
        if (!(orderComparator instanceof ContentComparator.NullComparator)) {
            results.sort((result1, result2) -> orderComparator.compare(
                            result1.localContent().orElse(null),
                            result2.localContent().orElse(null)
                    )
            );
        }

        return results;
    }

    /**
     * This method analyzes the server contents and compares them with the local files using a
     * content comparator to determine the appropriate removal actions. It returns a list of
     * PushAnalysisResult objects that represent the analysis results.
     *
     * @param localFiles     a list of local files to be compared against the server contents
     * @param serverContents a list of server contents to be analyzed
     * @param comparator     a content comparator used to compare server content with local files
     * @return a list of PushAnalysisResult objects representing the analysis results
     */
    private <T> List<PushAnalysisResult<T>> checkServerForRemovals(List<File> localFiles,
            List<T> serverContents, ContentComparator<T> comparator) {

        if (serverContents.isEmpty()) {
            return Collections.emptyList();
        }

        // Convert List<File> to List<T>
        List<T> localContents = map(localFiles, comparator.type());

        List<PushAnalysisResult<T>> removals = new ArrayList<>();

        for (T serverContent : serverContents) {

            var localMatch = comparator.existMatchingLocalContent(
                    serverContent, localFiles, localContents
            );
            if (!localMatch) {
                removals.add(
                        PushAnalysisResult.<T>builder().
                                action(PushAction.REMOVE).
                                serverContent(serverContent).
                                build()
                );
            }
        }

        return removals;
    }

    /**
     * This method reads the contents of a local file or directory and returns a list of File
     * objects representing the evaluated files.
     *
     * @param localFile the local file or directory to be read
     * @return a list of File objects representing the evaluated files
     */
    private List<File> readLocalContents(File localFile) {

        if (localFile.isFile() && !localFile.isHidden()) {
            return List.of(localFile);
        } else if (localFile.isDirectory()) {
            var foundFiles = localFile.listFiles(new HiddenFileFilter());
            if (foundFiles != null) {
                return List.of(foundFiles);
            }
        }

        return new ArrayList<>();
    }

    /**
     * This method maps a given local file to the specified class using the mapper service and
     * returns the mapped object.
     *
     * @param localFile the local file to be mapped
     * @param clazz     the class to map the local file onto
     * @return the mapped object of type T
     */
    private <T> T map(File localFile, Class<T> clazz) {
        return mapperService.map(localFile, clazz);
    }

    /**
     * This method maps a list of local files to the specified class using the mapper service and
     * returns a list of the mapped objects.
     *
     * @param localFiles the list of local files to be mapped
     * @param clazz      the class to map the local files onto
     * @return a list of the mapped objects of type T
     */
    private <T> List<T> map(List<File> localFiles, Class<T> clazz) {

        return localFiles.stream()
                .map(file -> map(file, clazz))
                .collect(Collectors.toList());
    }

}
