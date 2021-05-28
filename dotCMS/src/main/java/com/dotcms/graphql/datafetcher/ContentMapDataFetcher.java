package com.dotcms.graphql.datafetcher;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.rest.ContentResource;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.contentlet.transform.strategy.BinaryViewStrategy;
import com.dotmarketing.portlets.contentlet.transform.strategy.FileAssetViewStrategy;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ContentMapDataFetcher implements DataFetcher<Object> {

    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String key = environment.getArgument("key");

            if(UtilMethods.isSet(key)) {
                return contentlet.get(key);
            }

            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            final User user = ((DotGraphQLContext) environment.getContext()).getUser();

            final DotContentletTransformer myTransformer = new DotTransformerBuilder()
                    .hydratedContentMapTransformer().content(contentlet).build();

            // TODO - crear mapa de FieldTypes and ViewStrategies
            // iterar por los fields del contentlet y dado su type obtener la vista y agregarla al mapa del contentlet
            //
//
//            return hydrateContentlet(reTransformedContentlet);
            final Map<String, Object> hydratedMap =  myTransformer.toMaps().get(0);


            final JSONObject contentMapInJSON = new JSONObject();
//

            // this only adds relationships to any json. We would need to return them with the transformations already

            final JSONObject jsonWithRels = ContentResource.addRelationshipsToJSON(request, response,
                    "false", user, 1, false, contentlet,
                    contentMapInJSON, null, 1, true, false);



            HashMap<String,Object> result = new ObjectMapper().readValue(jsonWithRels.toString(), HashMap.class);
            hydratedMap.putAll(result);

            return hydratedMap;

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

//    private Contentlet hydrateContentlet(final Contentlet contentlet) {
//
//        final List<Field> binaries = contentlet.getContentType().fields(BinaryField.class);
//        if (!binaries.isEmpty()) {
//            for (final Field field : binaries) {
//                contentlet.getMap().put(field.variable(), BinaryViewStrategy.transform(field, contentlet));
//            }
//        }
//
//        final List<Field> fields = contentlet.getContentType().fields(FileField.class);
//        if (!fields.isEmpty()) {
//            for (final Field field : fields) {
//                new FileAssetViewStrategy(new APIProvider.Builder().build())
//                contentlet.getMap().put(field.variable(), BinaryViewStrategy.transform(field, contentlet));
//            }
//        }
//
//
//        return contentlet;
//
//    }
}
