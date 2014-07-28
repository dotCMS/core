package com.dotmarketing.util;

import com.dotcms.repackage.hibernate.net.sf.hibernate.collection.Map;
import com.dotcms.repackage.xstream.com.thoughtworks.xstream.converters.collections.MapConverter;
import com.dotcms.repackage.xstream.com.thoughtworks.xstream.mapper.Mapper;

public class HibernateMapConverter extends MapConverter {

    public HibernateMapConverter(Mapper mapper) {
        super(mapper);
    }

    public boolean canConvert(Class type) {
        return super.canConvert(type) || type == Map.class; 
    }
}