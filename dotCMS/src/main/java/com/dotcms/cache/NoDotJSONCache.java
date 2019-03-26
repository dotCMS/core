package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.liferay.portal.model.User;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

/** No Cache implementation for HTTP methods that don't require cache */
public class NoDotJSONCache extends DotJSONCache {
  @Override
  public String getPrimaryGroup() {
    return null;
  }

  @Override
  public String[] getGroups() {
    return new String[0];
  }

  @Override
  public void clearCache() {}

  @Override
  public void add(final HttpServletRequest request, final User user, final DotJSON dotJSON) {
    // no implementation
  }

  @Override
  public Optional<DotJSON> get(final HttpServletRequest request, final User user) {
    return Optional.empty();
  }

  @Override
  public void remove(final DotJSONCacheKey dotJSONCacheKey) {
    // no implementation
  }
}
