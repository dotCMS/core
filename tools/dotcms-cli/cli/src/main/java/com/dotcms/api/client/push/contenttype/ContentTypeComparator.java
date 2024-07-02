package com.dotcms.api.client.push.contenttype;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.ContentComparator;
import com.dotcms.contenttype.model.type.ContentType;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Optional;


@Dependent
public class ContentTypeComparator implements ContentComparator<ContentType> {

    @Inject
    protected RestClientFactory clientFactory;

    @Override
    public Class<ContentType> type() {
        return ContentType.class;
    }

    @ActivateRequestContext
    @Override
    public Optional<ContentType> findMatchingServerContent(File localFile,
            ContentType localContentType, List<ContentType> serverContents) {

        // Compare by identifier first.
        var result = findById(localContentType.id(), serverContents);

        if (result.isEmpty()) {

            // If not found by id, compare by variable name.
            result = findByVarName(localContentType.variable(), serverContents);
        }

        return result;
    }

    @ActivateRequestContext
    @Override
    public boolean existMatchingLocalContent(ContentType serverContent, List<File> localFiles,
            List<ContentType> localSites) {

        // Compare by identifier first.
        var result = findById(serverContent.id(), localSites);

        if (result.isEmpty()) {

            // If not found by id, compare by variable name.
            result = findByVarName(serverContent.variable(), localSites);
        }

        return result.isPresent();
    }

    @ActivateRequestContext
    @Override
    public boolean contentEquals(ContentType localContentType, ContentType serverContent) {

        // Searching for the content type in the server, this extra call is because the
        // "getContentTypes" api call returns content types without fields, without fields we
        // can't compare properly the content types
        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
        final var response = contentTypeAPI.getContentType(serverContent.variable(),
                null, null);
        serverContent = response.entity();

        // Comparing the local and server content in order to determine if we need to update or
        // not the content
        return localContentType.equals(serverContent);
    }

    /**
     * Finds a ContentType object in the given list based on the specified id.
     *
     * @param id           the identifier of the ContentType object to be found
     * @param contentTypes the list of ContentTypes objects to search in
     * @return an Optional containing the found ContentType object, or an empty Optional if no match
     * is found
     */
    private Optional<ContentType> findById(String id, List<ContentType> contentTypes) {

        if (id != null && !id.isEmpty()) {
            for (var contentType : contentTypes) {
                if (contentType.id() != null && contentType.id().equals(id)) {
                    return Optional.of(contentType);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Finds a ContentType object in the given list based on the specified variable name.
     *
     * @param varName      the variable name of the ContentType object to be found
     * @param contentTypes the list of ContentType objects to search in
     * @return an Optional containing the found ContentType object, or an empty Optional if no match
     * is found
     */
    private Optional<ContentType> findByVarName(String varName, List<ContentType> contentTypes) {

        if (varName != null && !varName.isEmpty()) {
            for (var contentType : contentTypes) {
                if (contentType.variable() != null && contentType.variable()
                        .equalsIgnoreCase(varName)) {
                    return Optional.of(contentType);
                }
            }
        }

        return Optional.empty();
    }

}
