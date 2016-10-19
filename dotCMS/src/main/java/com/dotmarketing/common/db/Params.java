package com.dotmarketing.common.db;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Encapsulate a list of parameters
 * @author jsanca
 */
public class Params implements Serializable {

    private final Object [] params;

    /**
     * Create params based on a list of parameter
     * @param params Object array
     */
    public Params(final Object... params) {
        this.params = params;
    }

    private Params(final Builder params) {

        this.params = params.paramList.toArray();
    }

    /**
     * Returns the size of the params
     * @return int
     */
    public final int size () {

        return this.params.length;
    }

    /**
     * Get the element associated to the "index", 0 to Size-1
     * If you use as an index out of bounds, will throw a {@link IndexOutOfBoundsException}
     * @param index int
     * @return Object
     */
    public final Object get(final int index) {

        if (index < 0 || index >= this.size()) {

            throw new IndexOutOfBoundsException("Index:" + index + ", is out of bounds");
        }

        return this.params[index];
    }

    public static final class Builder {

        private List<Object> paramList = new ArrayList<>();

        public Builder add (final Object... parameters) {

            if (null != parameters && parameters.length > 0) {

                if (1 == parameters.length) {

                    this.paramList.add(parameters[0]);
                } else {

                    for (Object p : parameters) {
                        this.paramList.add(p);
                    }
                }
            }

            return this;
        } // add.

        public Builder add (final Collection<Object> collection) {

            if (null != collection && collection.size() > 0) {

                this.paramList.addAll(collection);
            }

            return this;
        } // add.

        public Params build () {

            return new Params(this);
        }
    }
} // E:O:F:Params.
