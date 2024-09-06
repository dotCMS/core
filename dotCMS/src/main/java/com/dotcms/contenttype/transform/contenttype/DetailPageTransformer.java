package com.dotcms.contenttype.transform.contenttype;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.net.URISyntaxException;
import java.util.Optional;

public interface DetailPageTransformer {

    /**
     * Converts a URI to an identifier, the detail page field supports both URIs and identifiers. If
     * the detail page is a URI, it will be converted to an identifier. If no detail page is set, or
     * it is an identifier, it will return an empty Optional.
     *
     * @return An Optional containing the identifier if a conversion was made, or an empty Optional
     * if it was not.
     * @throws URISyntaxException   If the detail page URL is not a valid URI.
     * @throws DotDataException     If there is an error accessing data.
     * @throws DotSecurityException If there is a security error.
     */
    Optional<String> uriToId() throws URISyntaxException, DotDataException, DotSecurityException;

    /**
     * Converts an identifier to a URI, the detail page field supports both URIs and identifiers. If
     * the detail page is an identifier, it will be converted to a URI. If no detail page is set, or
     * it is not an identifier, it will return an empty Optional.
     *
     * @return An Optional containing the URI if a conversion was made, or an empty Optional if it
     * was not.
     * @throws DotDataException     If there is an error accessing data.
     * @throws DotSecurityException If there is a security error.
     */
    Optional<String> idToUri() throws DotDataException, DotSecurityException;

}
