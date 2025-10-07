package com.dotcms.rendering.js.proxy;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyArray;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View tool to retrieve on the execute function the roles in a lazy approach
 * @author jsanca
 */
public class JsRolesFetcherByUser  implements Serializable {

    private final String userId;

    public JsRolesFetcherByUser(final String userId) {
        this.userId = userId;
    }

    @HostAccess.Export
    public Object asArray() {
        try {
            Logger.debug(this, ()-> "Getting the roles of userId: " + userId);
            return ProxyArray.fromArray(APILocator.getRoleAPI().loadRolesForUser(
                    userId).stream().map(JsRole::new).toArray(JsRole[]::new));
        } catch (Exception e) {

            Logger.error(e.getMessage(), e);
        }

        return new JsRole [] {};
    }

    @HostAccess.Export
    public Object asList() {
        try {
            Logger.debug(this, ()-> "Getting the roles of userId");
            return JsProxyFactory.createProxy(
                APILocator.getRoleAPI().loadRolesForUser(
                        userId).stream().map(JsRole::new).collect(Collectors.toList()));
        } catch (Exception e) {

            Logger.error(e.getMessage(), e);
        }

        return List.of();
    }
}
