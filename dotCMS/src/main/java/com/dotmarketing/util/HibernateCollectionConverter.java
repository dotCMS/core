package com.dotmarketing.util;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;
import com.dotcms.repackage.net.sf.hibernate.collection.List;
import com.dotcms.repackage.net.sf.hibernate.collection.Set;

public class HibernateCollectionConverter extends CollectionConverter {
    public HibernateCollectionConverter(Mapper mapper) {
        super(mapper);
    }

    public boolean canConvert(Class type) {
        return super.canConvert(type) || type == List.class || type == Set.class; 
    }
}