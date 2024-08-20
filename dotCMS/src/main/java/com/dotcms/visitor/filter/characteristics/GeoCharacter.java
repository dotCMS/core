package com.dotcms.visitor.filter.characteristics;


import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.visitor.domain.Geolocation;
import com.dotmarketing.util.Logger;
import javax.servlet.http.HttpSession;

public class GeoCharacter extends AbstractCharacter {

    private final static String CLICKHOUSE_GEOLOCATION_ATTR = "CLICKHOUSE_GEOLOCATION_ATTR";

    public GeoCharacter(AbstractCharacter incomingCharacter) {
        super(incomingCharacter);


        try {
            HttpSession session = request.getSession(false);
            Geolocation geo = session != null ? (Geolocation) session.getAttribute(CLICKHOUSE_GEOLOCATION_ATTR) : null;

            geo = geo != null ? geo : new Geolocation.Builder().build();

            if (geo.getLatitude() == 0 && geo.getLongitude() == 0) {
                geo = GeoIp2CityDbUtil.getInstance().getGeolocation(request.getRemoteAddr());
            }
            if (session != null) {
                session.setAttribute(CLICKHOUSE_GEOLOCATION_ATTR, geo);
            }

            float[] latLong = parseLatLong(((String) getMap().get("g.latLong")));
            accrue("geo_lat", latLong[0]);
            accrue("geo_lon", latLong[1]);
            accrue("geo_country", getMap().get("g.countryCode"));
            accrue("geo_city", getMap().get("g.cityName"));
            accrue("geo_continent", getMap().get("g.continent"));
            accrue("geo_company", getMap().get("g.company"));
            accrue("geo_subdivision", getMap().get("g.subdivisionCode"));




        } catch (Exception e) {
            Logger.warnEveryAndDebug(this.getClass(), e, 10000);
        }

    }

    protected float[] parseLatLong(final String latLong) {
      float[] latLongFloat = new float[] {0f, 0f};
      try {
        final String[] latLongArray = latLong.split("[, ]");
        latLongFloat[0] = Float.parseFloat(latLongArray[0]);
        latLongFloat[1] = Float.parseFloat(latLongArray[1]);
      } catch (Exception e) {
        Logger.debug(this.getClass(), "Unable to parse lat long:" + e.getMessage());
      }
      return latLongFloat;
    }
}
