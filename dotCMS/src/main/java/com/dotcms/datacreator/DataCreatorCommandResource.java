package com.dotcms.datacreator;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import com.google.common.reflect.ClassPath;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Path("/api/datacreator")
public class DataCreatorCommandResource {

    private static final Lazy<Boolean> isTestMode = Lazy.of(()-> Config.getBooleanProperty("IS_TEST_MODE", false));
    private final WebResource webResource = new WebResource();
    private final Map<String, DataCreatorCommand> creatorsMap = this.initCreatorsMap ();

    private Map<String, DataCreatorCommand> initCreatorsMap() {

        final Map<String, DataCreatorCommand> map = new HashMap<>();

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            for (final ClassPath.ClassInfo info : ClassPath.from(loader).getTopLevelClasses()) {
                if (info.getName().startsWith("com.dotcms.datacreator.creators.")) {
                    final Class<?> clazz = info.load();
                    clazz.isAssignableFrom(DataCreatorCommand.class);
                    final DataCreatorCommand dataCreatorCommand = (DataCreatorCommand)ReflectionUtils.newInstance(clazz);
                    map.put(dataCreatorCommand.getName(), dataCreatorCommand);
                }
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }

        return map;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> executeCreators(@Context HttpServletRequest request, @Context final HttpServletResponse response,
                                          @QueryParam("creators") final String creators) {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .init().getUser();

        if (!this.isRunningTestMode()) {

            throw new IllegalArgumentException("Creators can be fired only on test mode");
        }

        if (UtilMethods.isSet(creators)) {

            throw new BadRequestException("The query string parameter 'creators' should be pass");
        }

        final Map<String, Object> contextMap = new HashMap<>();
        final String [] creatorsArray = creators.split(StringPool.COMMA);
        Stream.of(creatorsArray).forEach(creator -> executeCreator (creator, contextMap));

        return contextMap;
    }

    private boolean isRunningTestMode() {

        return isTestMode.get();
    }

    private void executeCreator(final String creator, final Map<String, Object> contextMap) {

        if (this.creatorsMap.containsKey(creator)) {

            this.creatorsMap.get(creator).execute(contextMap);
        }
    }
}
