package com.dotmarketing.util;

import com.dotcms.repackage.hibernate2.net.sf.hibernate.collection.List;
import com.dotcms.repackage.hibernate2.net.sf.hibernate.collection.Set;
import com.dotcms.repackage.xstream_1_4_4.com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.dotcms.repackage.xstream_1_4_4.com.thoughtworks.xstream.mapper.Mapper;

public class HibernateCollectionConverter extends CollectionConverter {
    public HibernateCollectionConverter(Mapper mapper) {
        super(mapper);
    }

    public boolean canConvert(Class type) {
        return super.canConvert(type) || type == List.class || type == Set.class; 
    }
}