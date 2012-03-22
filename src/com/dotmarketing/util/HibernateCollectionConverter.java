package com.dotmarketing.util;

import net.sf.hibernate.collection.List;
import net.sf.hibernate.collection.Set;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;

public class HibernateCollectionConverter extends CollectionConverter {
    public HibernateCollectionConverter(Mapper mapper) {
        super(mapper);
    }

    public boolean canConvert(Class type) {
        return super.canConvert(type) || type == List.class || type == Set.class; 
    }
}