package com.dotcms.visitor.business;

import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.web.LanguageWebAPI;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface VisitorAPI {



    Optional<Visitor> getVisitor(HttpServletRequest request);

    Optional<Visitor> getVisitor(HttpServletRequest request, boolean create);

    void removeVisitor(HttpServletRequest request);
}
