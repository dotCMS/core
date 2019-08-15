package com.dotmarketing.portlets.workflows.business;

import java.util.Collection;
import java.util.Iterator;

public class SystemActionMappingReferenceCache {

    private final TYPE type;
    private final String [] keys;

    private SystemActionMappingReferenceCache(final Builder builder) {
        this.keys = builder.keys;
        this.type = builder.type;
    }

    public TYPE getType() {
        return type;
    }

    public String getKey() {

        return null != keys && keys.length > 0? keys[0]:null;
    }
    public String[] getKeys() {
        return keys;
    }

    public static final class Builder {

        TYPE type;
        String[] keys;

        public Builder workflowId (final String workflowId) {

            this.type = TYPE.WORKFLOW;
            keys      = new String[] {workflowId};
            return this;
        }

        public Builder contentTypeVar (final String variable) {

            this.type = TYPE.CONTENTTYPE;
            keys      = new String[] {variable};
            return this;
        }

        public Builder schemeId (final String schemeId) {

            this.type = TYPE.SCHEME;
            keys      = new String[] {schemeId};
            return this;
        }

        public Builder systemActionByContentType (final String systemActionName, final String variable) {

            this.type = TYPE.SYSTEMACTION_CONTENTTYPE;
            keys      = new String[] {systemActionName, variable};
            return this;
        }

        public Builder systemActionBySchemeIds (final String systemActionName, final String... schemeIds) {

            this.type = TYPE.SYSTEMACTION_SCHEMES;
            keys      = new String[schemeIds.length+1];
            keys[0]   = systemActionName;
            for (int i = 0; i < schemeIds.length; ++i) {

                keys[i+1] = schemeIds[i];
            }
            return this;
        }

        public Builder systemActionBySchemeIds (final String systemActionName, final Collection<String> schemeIds) {

            this.type = TYPE.SYSTEMACTION_SCHEMES;
            keys      = new String[schemeIds.size()+1];
            keys[0]   = systemActionName;
            int i = 1;
            final Iterator<String> iterator = schemeIds.iterator();
            while (iterator.hasNext()) {

                keys[i++] = iterator.next();
            }

            return this;
        }

        public SystemActionMappingReferenceCache build() {

            return new SystemActionMappingReferenceCache(this);
        }
    }

    enum TYPE {
        WORKFLOW, CONTENTTYPE, SCHEME, SYSTEMACTION_CONTENTTYPE, SYSTEMACTION_SCHEMES
    }
}
