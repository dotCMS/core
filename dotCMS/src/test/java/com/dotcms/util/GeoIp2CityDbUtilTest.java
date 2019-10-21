package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.visitor.domain.Geolocation;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GeoIp2CityDbUtilTest {


    final String geoJSON = "{\"latitude\":42.6489,\"longitude\":-71.1655,\"country\":\"United States\",\"countryCode\":\"US\",\"city\":\"Andover\",\"continent\":\"North America\",\"continentCode\":\"NA\",\"company\":null,\"timezone\":\"America/New_York\",\"subdivision\":\"Massachusetts\",\"subdivisionCode\":\"MA\",\"ipAddress\":null,\"latLong\":\"42.6489,-71.1655\"}";
    @BeforeClass
    public static void prepare() throws Exception {


    }


    @Test
    public void test_geo_db_is_singleton() throws Exception {

        String current = new java.io.File(".").getCanonicalPath();
        System.out.println("Current dir:" + current);
        GeoIp2CityDbUtil geoUtil = GeoIp2CityDbUtil.getInstance();
        assertNotNull("we have a GeoDb", geoUtil);
        assertTrue("GeoDb is a singleton", geoUtil == GeoIp2CityDbUtil.getInstance());

    }


    @Test
    public void test_geo_db_works() throws Exception{
        final String ipAddress="108.49.249.66";
        ObjectMapper mapper = new ObjectMapper();
        Geolocation geo = GeoIp2CityDbUtil.getInstance().getGeolocation(ipAddress);
        assertNotNull("we have a geo", geo);
        System.out.println(mapper.writeValueAsString(geo));
        assertEquals(geo.getCity(), "Andover");
        assertEquals(geo.getCountry(), "United States");
        assertEquals(new Double(geo.getLatitude()), new Double(42.6489));
        assertEquals(new Double(geo.getLongitude()), new Double(-71.1655d));
        assertEquals(geo.getCountryCode(),"US");
        assertEquals(geo.getContinent(),"North America");
        assertEquals(geo.getContinentCode(),"NA");
        assertEquals(geo.getTimezone(),"America/New_York");
        assertEquals(geo.getSubdivision(), "Massachusetts");
        assertEquals(geo.getSubdivisionCode(), "MA");
        assertEquals(geo.getIpAddress(), ipAddress);
    }

    @Test
    public void test_Geolocation_can_serialize() throws Exception{

        ObjectMapper mapper = new ObjectMapper();
        Geolocation orig = GeoIp2CityDbUtil.getInstance().getGeolocation("108.49.249.66");
        assertNotNull("we have a geo", orig);
        
        String geoJson = mapper.writeValueAsString(orig);
        
        
        Geolocation fromJson = mapper.readValue(geoJson, Geolocation.class);
        
        assertEquals(orig, fromJson);

    }


}
