package com.dotcms.rendering.velocity.viewtools;

public class ToolResponse<T>{

    private final T entity;
    private final String errorMessage;

    private ToolResponse(T entity, String errorMessage) {
        this.entity = entity;
        this.errorMessage = errorMessage;
    }

    public T getEntity() {
        return entity;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    static class Builder<E> {
        private E entity;
        private String errorMessage;

        Builder<E> entity(final E entity) {
            this.entity = entity;
            return this;
        }

        Builder<E> errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        ToolResponse<E> build() {
            return new ToolResponse<>(entity, errorMessage);
        }
    }
}
