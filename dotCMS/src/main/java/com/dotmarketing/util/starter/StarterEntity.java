package com.dotmarketing.util.starter;

import com.fasterxml.jackson.core.type.TypeReference;

final class StarterEntity {

    private final String fileName;
    private final Object type;

    private StarterEntity(final Builder builder){
        this.fileName = builder.fileName;
        this.type = builder.type;
    }

    public String fileName(){
        return fileName;
    }

    public Object type(){
        return type;
    }

    public static class Builder {

        private String fileName;
        private Object type;

        public static Builder newInstance()
        {
            return new Builder();
        }

        private Builder() {}

        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setType(Object type) {
            this.type = type;
            return this;
        }

        public StarterEntity build(){
            return new StarterEntity(this);
        }
    }
}
