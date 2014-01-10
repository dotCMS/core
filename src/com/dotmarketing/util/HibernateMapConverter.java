package com.dotmarketing.util;

import com.dotcms.repackage.hibernate2.net.sf.hibernate.collection.Map;
import com.dotcms.repackage.xstream_1_4_4.com.thoughtworks.xstream.converters.collections.MapConverter;
import com.dotcms.repackage.xstream_1_4_4.com.thoughtworks.xstream.mapper.Mapper;

public class HibernateMapConverter extends MapConverter {

    public HibernateMapConverter(Mapper mapper) {
        super(mapper);
    }

    public boolean canConvert(Class type) {
        return super.canConvert(type) || type == Map.class; 
    }
}