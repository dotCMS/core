package com.dotcms.visitor.filter.characteristics;

import com.dotcms.util.GeoIp2CityDbUtil;
import com.google.common.collect.ImmutableMap;

public class GeoCharacter extends AbstractCharacter {



    private final static String VISITOR_PLUGIN_GEOLOCATION = "VISITOR_PLUGIN_GEOLOCATION";


    public GeoCharacter(AbstractCharacter incomingCharacter) {
        super(incomingCharacter);

        ImmutableMap<String, String> m;
        if (visitor.get(VISITOR_PLUGIN_GEOLOCATION) != null && visitor.get(VISITOR_PLUGIN_GEOLOCATION) instanceof ImmutableMap) {
            m = (ImmutableMap<String, String>) visitor.get(VISITOR_PLUGIN_GEOLOCATION);
        } else {

            try {
                String ipAddress = visitor.getIpAddress().getHostAddress();
                GeoIp2CityDbUtil geo = GeoIp2CityDbUtil.getInstance();
                m = new ImmutableMap.Builder<String, String>().put("g.latLong", geo.getLocationAsString(ipAddress))
                    .put("g.countryCode", geo.getCountryIsoCode(ipAddress))
                    .put("g.cityName", geo.getCityName(ipAddress))
                    .put("g.continent", geo.getContinent(ipAddress))
                    .put("g.company", geo.getCompany())
                    .build();
            } catch (Exception e) {
                m = ImmutableMap.of("g.ip", "ukn");
            }

            visitor.put(VISITOR_PLUGIN_GEOLOCATION, m);
        }
        getMap().putAll(m);

    }

}
