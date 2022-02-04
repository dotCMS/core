package com.dotcms.filters.interceptor.meta;

import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.StringUtils;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Interceptor that adds meta info to the dotcms response
 *
 * includes:
 *  - x-dot-server header, contains the node name + server id (this helps to identified on a cluster context which node has committed the response)
 *
 * @author jsanca
 */
public class ResponseMetaDataWebInterceptor implements WebInterceptor {

    public static final String RESPONSE_HEADER_ADD_NODE_ID = "RESPONSE_HEADER_ADD_NODE_ID";
    public static final String RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME = "RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME";
    public static final String X_DOT_SERVER_HEADER = "x-dot-server";

    private static final String FRIENDLY_NAME = "friendlyName";
    private static final String UNKNOWN = "unknown";
    private static final String NODEID_PARAM = "nodeid";
    private static final String TRUE_VALUE = "true";

    private final Lazy<Boolean> responseHeaderAddNodeId = Lazy.of(()-> Config.getBooleanProperty(RESPONSE_HEADER_ADD_NODE_ID, true));
    private final Lazy<String>  serverId                = Lazy.of(()-> StringUtils.shortify(APILocator.getServerAPI().readServerId(), 10));
    private final Lazy<String>  nodeName                = Lazy.of(this::getNodeName);

    private final String getNodeName () {

        if (Config.getBooleanProperty(RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME, true)) {

            final Map<String, Serializable> nodeInfoMap = Try.of(() -> ClusterUtilProxy.getNodeInfo()).getOrElse(Collections.emptyMap());
            return nodeInfoMap.getOrDefault(FRIENDLY_NAME, UNKNOWN).toString();
        }

        return UNKNOWN;
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {

        if (responseHeaderAddNodeId.get() ||
                TRUE_VALUE.equals(request.getParameter(NODEID_PARAM))) {

            response.addHeader(X_DOT_SERVER_HEADER,
                            nodeName.get()
                            + StringPool.PIPE
                            + serverId.get()
            );
        }

        return Result.NEXT;
    }

} // E:O:F:MetaWebInterceptor.
