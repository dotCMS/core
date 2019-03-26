package com.dotcms.visitor.filter.geo;

import com.dotcms.visitor.domain.Visitor;
import java.util.Map;

public interface GeoLocationProvider {

  Map<String, String> getGeoInfo(Visitor visitor);
}
