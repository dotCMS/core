package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;

class IdentifierViewStrategy extends AbstractTransformStrategy<Contentlet>{

    IdentifierViewStrategy(final TransformToolbox toolBox) {
        super(toolBox);
    }

    @Override
    protected Map<String, Object> transform(final Contentlet contentlet, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user) throws DotDataException {

        final Identifier identifier = toolBox.identifierAPI.find(contentlet.getIdentifier());
        map.putAll(mapIdentifier(identifier,true));
        return map;
    }

    /**
     * Mapping Lang functions
     * @param identifier
     * @param wrapAsMap
     * @return
     */
    Map<String, Object> mapIdentifier(final Identifier identifier, final boolean wrapAsMap) {

        final Builder<String, Object> builder = new Builder<>();
        builder
                .put("identifier", identifier.getId())
                .put("parentPath", identifier.getParentPath())
                .put("hostId", identifier.getHostId());

        if(wrapAsMap){
            builder.put("id", identifier.getId());
            return ImmutableMap.of("identifierMap", builder.build(), "identifier",identifier.getId());
        }
        return builder.build();
    }

}
