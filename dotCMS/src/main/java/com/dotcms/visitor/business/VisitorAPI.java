package com.dotcms.visitor.business;

import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.web.LanguageWebAPI;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

public interface VisitorAPI {

  void setLanguageWebAPI(LanguageWebAPI languageWebAPI);

  Optional<Visitor> getVisitor(HttpServletRequest request);

  Optional<Visitor> getVisitor(HttpServletRequest request, boolean create);
}
