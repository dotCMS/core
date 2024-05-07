package com.dotcms.languagevariable.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import java.util.List;

public interface LanguageVariableFactory {

    List<LanguageVariable> findVariables(ContentType contentType, long languageId, int offset,
            int limit, String sortBy) throws DotDataException;

    List<LanguageVariableExt> findVariablesForPagination(ContentType contentType, final int offset, final int limit, final String orderBy) throws DotDataException;

    int countVariablesByIdentifier(ContentType contentType) throws DotDataException;

}
