package com.dotmarketing.portlets.workflows.business;

import org.apache.commons.collections.keyvalue.MultiKey;

public class SystemActionMappingReferenceCacheKey  {

    private final TYPE type;
    private final String [] keys;

    private SystemActionMappingReferenceCacheKey(final Builder builder) {
        super(builder.keys);
        this.type = builder.type;
    }

    public TYPE getType() {
        return type;
    }

    public static final class Builder {

        TYPE type;
        Object[] keys;

        public Builder workflowId (final String workflowId) {

            this.type = TYPE.WORKFLOW;
            keys      = new Object[] {workflowId};
            return this;
        }

        public Builder contentTypeVar (final String variable) {

            this.type = TYPE.CONTENTTYPE;
            keys      = new Object[] {variable};
            return this;
        }

        public Builder schemeId (final String schemeId) {

            this.type = TYPE.SCHEME;
            keys      = new Object[] {schemeId};
            return this;
        }

        public Builder systemActionByContentType (final String systemActionName, final String variable) {

            this.type = TYPE.SYSTEMACTION_CONTENTTYPE;
            keys      = new Object[] {systemActionName, variable};
            return this;
        }

        public Builder systemActionByContentType (final String systemActionName, final String... schemeIds) {

            this.type = TYPE.SYSTEMACTION_SCHEMES;
            keys      = schemeIds;
            return this;
        }

        public SystemActionMappingReferenceCacheKey build() {

            return new SystemActionMappingReferenceCacheKey(this);
        }
    }

    enum TYPE {
        WORKFLOW, CONTENTTYPE, SCHEME, SYSTEMACTION_CONTENTTYPE, SYSTEMACTION_SCHEMES
    }
}
