package com.dotcms.visitor.business;

import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface VisitorAPI {

    void setLanguageAPI(LanguageAPI languageAPI);

    Optional<Visitor> getVisitor(HttpServletRequest request);

    Optional<Visitor> getVisitor(HttpServletRequest request, boolean create);

}
