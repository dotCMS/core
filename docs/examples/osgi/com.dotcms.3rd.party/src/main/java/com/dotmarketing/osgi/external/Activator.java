package com.dotmarketing.osgi.external;

import com.dotmarketing.osgi.GenericBundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Created by Jonathan Gamba
 * Date: 6/18/12
 */
public class Activator extends GenericBundleActivator {

    @SuppressWarnings ("unchecked")
    public void start ( BundleContext context ) throws Exception {

        //RUNNING EXAMPLES USING OUR 3RD PARTY JAR (date4j.jar -> http://www.date4j.net/)
        Examples.currentDateTime();
        Examples.currentDateTimeInCairo();
        Examples.ageIfBornOnCertainDate();
        Examples.optionsExpiry();
        Examples.daysTillChristmas();
        Examples.whenIs90DaysFromToday();
        Examples.whenIs3Months5DaysFromToday();
        Examples.hoursDifferenceBetweenParisAndPerth();
        Examples.weeksSinceStart();
        Examples.timeTillMidnight();
        Examples.imitateISOFormat();
        Examples.firstDayOfThisWeek();
        Examples.jdkDatesSuctorial();
    }

    public void stop ( BundleContext context ) throws Exception {
    }

}