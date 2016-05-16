package com.dotcms.datagen;

public interface DataGen<T> {

    T next();

    T persist(T object);

    void remove(T object);

    default T nextPersisted() {
        return persist(next());
    }
}
