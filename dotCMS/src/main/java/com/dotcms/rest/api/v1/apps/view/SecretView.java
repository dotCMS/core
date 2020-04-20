package com.dotcms.rest.api.v1.apps.view;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.security.apps.AbstractProperty;
import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonSerialize(using = SecretView.SecretViewSerializer.class)
public class SecretView {

    private final String name;

    final private Secret secret;

    final private ParamDescriptor paramDescriptor;

    final private boolean dynamic;

    public SecretView(final String name, final Secret secret, final ParamDescriptor paramDescriptor) {
        this.name = name;
        this.secret = secret;
        this.paramDescriptor = paramDescriptor;
        this.dynamic = null == paramDescriptor;
    }

    public String getName() {
        return name;
    }

    public Secret getSecret() {
        return secret;
    }

    public ParamDescriptor getParamDescriptor() {
        return paramDescriptor;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SecretView that = (SecretView) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * This serializer generates the Json output for SecretView
     * It'll render a secret or a paramDescriptor
     * But if both are set the generated output will show them both merged.
     * The two objects share common properties defined by the AbstractProperty class.
     */
    public static class SecretViewSerializer extends JsonSerializer<SecretView> {

        @VisibleForTesting
        public static final String HIDDEN_SECRET_MASK = "*****";

        static final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
                .getDefaultObjectMapper();

        @Override
        public void serialize(final SecretView value, final JsonGenerator jsonGenerator,
                final SerializerProvider serializers) throws IOException {
            final Map<String, Object> map = new HashMap<>();
            map.put("name", value.getName());
            map.put("dynamic", value.isDynamic());
            final Secret secret = value.getSecret();
            final ParamDescriptor paramDescriptor = value.getParamDescriptor();
            if (null != secret && null != paramDescriptor) {
                mergeSecretAndParam(secret, paramDescriptor, map);
            } else {
                if (null != secret) {
                    buildSecret(secret, map);
                }
                if (null != paramDescriptor) {
                    buildParam(paramDescriptor, map);
                }
            }
            final String json = mapper.writeValueAsString(map);
            jsonGenerator.writeRawValue(json);
        }

        private void buildCommonJson(final AbstractProperty property,
                final Map<String, Object> map) {
            final Type type = property.getType();
            map.put("type", type);
            map.put("hidden", property.isHidden());
            if (type.equals(Type.BOOL)) {
                map.put("value", property.getBoolean());
            } else {
                map.put("value", property.isHidden() ? HIDDEN_SECRET_MASK : property.getString());
            }
        }

        private void buildSecret(final Secret secret,
                final Map<String, Object> map) {
            buildCommonJson(secret, map);
        }

        private void buildParam(final ParamDescriptor paramDescriptor,
                final Map<String, Object> map) {
            buildCommonJson(paramDescriptor, map);
            map.put("hint", paramDescriptor.getHint());
            map.put("label", paramDescriptor.getLabel());
        }

        private void mergeSecretAndParam(final Secret secret,
                final ParamDescriptor paramDescriptor,
                final Map<String, Object> map) {
            buildParam(paramDescriptor, map);
            buildCommonJson(secret, map); //call this at the end so the values from secret override
        }
    }

}
