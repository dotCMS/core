package com.dotcms.ai.domain;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents an AI model with a name, status, and index.
 *
 * <p>
 * This class encapsulates the details of an AI model, including its name, status, and index.
 * It provides methods to retrieve and set these details, as well as methods to check if the model is operational.
 * </p>
 *
 * <p>
 * The class also provides a builder for constructing instances of the model.
 * </p>
 *
 * @author vico
 */
public class Model {

    private final String name;
    private final AtomicReference<ModelStatus> status;
    private final AtomicInteger index;

    private Model(final Builder builder) {
        name = builder.name;
        status = new AtomicReference<>(null);
        index = new AtomicInteger(builder.index);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public ModelStatus getStatus() {
        return status.get();
    }

    public void setStatus(final ModelStatus status) {
        this.status.set(status);
    }

    public int getIndex() {
        return index.get();
    }

    public void setIndex(final int index) {
        this.index.set(index);
    }

    public boolean isOperational() {
        return ModelStatus.ACTIVE == status.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Model model = (Model) o;
        return Objects.equals(name, model.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "Model{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", index=" + index.get() +
                '}';
    }

    public static class Builder {

        private String name;
        private int index;

        public Builder withName(final String name) {
            this.name = name.toLowerCase().trim();
            return this;
        }

        public Builder withIndex(final int index) {
            this.index = index;
            return this;
        }

        public Model build() {
            return new Model(this);
        }

    }

}
