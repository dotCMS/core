package com.dotmarketing.common.db;

import io.vavr.control.Try;
import org.postgresql.util.PGobject;

import java.io.Serializable;

/**
 * Wrapper class with a utility Builder for {@link PGobject}
 */
public class DotPGobject implements Serializable {

    public static final class Builder {

        private String type;
        private String value;

        /**
         * Sets the type of this object
         *
         * @param type a string describing the type of the object
         */
        public Builder type(final String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the value of this object
         *
         * @param value a string representation of the value of the object
         */
        public Builder value(final String value) {
            this.value = value;
            return this;
        }

        /**
         * Sets the value of this object and the type to "json"
         *
         * @param jsonValue a string representation of the value of the object
         */
        public Builder jsonValue(final String jsonValue) {
            this.type = "json";
            this.value = jsonValue;
            return this;
        }

        public PGobject build() {

            var pgObject = new PGobject();
            pgObject.setType(this.type);
            Try.run(() -> pgObject.setValue(this.value)).getOrElseThrow(
                    () -> new IllegalArgumentException(String.format("Invalid argument of type [%s]", this.type)));

            return pgObject;
        }
    }
}

