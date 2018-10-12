package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface DotJSONCacheStrategy {

    void addIfNeeded(final HttpServletRequest request, final User user, final DotJSON dotJSON);

    Optional<DotJSON> get(final HttpServletRequest request, final User user);
}
