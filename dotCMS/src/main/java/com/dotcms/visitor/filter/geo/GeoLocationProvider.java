package com.dotcms.visitor.filter.geo;

import java.util.Map;

import com.dotcms.visitor.domain.Visitor;

public interface GeoLocationProvider {

    Map<String, String> getGeoInfo(Visitor visitor);

}
