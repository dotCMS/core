package com.dotcms.datagen;

public interface DataGen<T> {
    /**
     * Returns a new non-persisted instance of {@link T}.
     *
     * @return the non-persisted instance
     */
    T next();

    /**
     * Persists the given {@link T} object to the underlying datasource.
     *
     * @param object the object to persist
     * @return the persisted object
     */
     T persist(T object);

    /**
     * Returns a new persisted instance of {@link T}.
     *
     * @return the persisted instance
     */
    default T nextPersisted() {
        return persist(next());
    }

}
