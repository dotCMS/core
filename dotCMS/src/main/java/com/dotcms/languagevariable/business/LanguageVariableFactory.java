package com.dotcms.languagevariable.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import java.util.List;

/**
 * LanguageVariableFactory is the Data Access Object (DAO) for LanguageVariable
 */
public interface LanguageVariableFactory {

    /**
     * Find all LanguageVariables for a given ContentType and Language
     * @param contentType the ContentType
     * @param languageId the Language ID
     * @param offset the offset
     * @param limit the limit
     * @param sortBy the sort by
     * @return a List of LanguageVariable
     * @throws DotDataException if an error occurs
     */
    List<LanguageVariable> findVariables(ContentType contentType, long languageId, int offset,
            int limit, String sortBy) throws DotDataException;

    /**
     * Find all LanguageVariables for a given ContentType
     * @param contentType the ContentType
     * @param offset the offset
     * @param limit the limit
     * @param orderBy the order by
     * @return a List of LanguageVariableExt
     * @throws DotDataException if an error occurs
     */
    List<LanguageVariableExt> findVariablesForPagination(ContentType contentType, final int offset, final int limit, final String orderBy) throws DotDataException;

    /**
     * Count the number of LanguageVariables for a given ContentType
     * @param contentType the LangVariables ContentType
     * @return the count
     * @throws DotDataException if an error occurs
     */
    int countVariablesByKey(ContentType contentType);

    /**
     * Count the number of LanguageVariables for a given ContentType and Language
     * @param contentType the LangVariables ContentType
     * @param languageId the Language ID
     * @return the count
     */
    int countVariablesByKey(ContentType contentType, final long languageId);

}
