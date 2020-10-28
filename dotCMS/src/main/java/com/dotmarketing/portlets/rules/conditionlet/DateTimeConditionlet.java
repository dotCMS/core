package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import org.apache.logging.log4j.util.Strings;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.ComparisonNotPresentException;
import com.dotmarketing.portlets.rules.exception.ComparisonNotSupportedException;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import com.dotmarketing.portlets.rules.parameter.display.*;
import com.dotmarketing.portlets.rules.parameter.type.*;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Map;

import static com.dotcms.repackage.com.google.common.base.Preconditions.checkState;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * This conditionlet allows to check the visitor's current date
 * and time when a page was requested. This {@link Conditionlet} provides a
 * drop-down menu with the available comparison mechanisms, and a text field to
 * enter the date and time to compare. This date/time parameter will be
 * expressed in milliseconds in order to avoid any format-related and time zone
 * issues.
 * <p>
 * The date and time of the request is determined by the IP address of the
 * client that issued the request. Geographic information is then retrieved via
 * the <a href="http://maxmind.github.io/GeoIP2-java/index.html">GeoIP2 Java
 * API</a>.
 *
 * @author Jose Castro
 * @version 1.0
 * @since 04-22-2015
 */

public class DateTimeConditionlet extends Conditionlet<DateTimeConditionlet.Instance> {

    private static final long serialVersionUID = 1L;

    public static final String DATE_TIME_1_KEY = "datetime-1";
    public static final String DATE_TIME_2_KEY = "datetime-2";

    private final GeoIp2CityDbUtil geoIp2Util;

    private static final ParameterDefinition<DateTimeType> dateTime1 = new ParameterDefinition<>(
            3, DATE_TIME_1_KEY,
            new DateTimeInput<>(new DateTimeType().required())
    );

    private static final ParameterDefinition<DateTimeType> dateTime2 = new ParameterDefinition<>(
            4, DATE_TIME_2_KEY,
            new DateTimeInput<>(new DateTimeType())
    );

    public DateTimeConditionlet() {
        this(GeoIp2CityDbUtil.getInstance());
    }

    @VisibleForTesting
    DateTimeConditionlet(GeoIp2CityDbUtil geoIp2Util) {
        super("api.ruleengine.system.conditionlet.VisitorsDateTime",
                new ComparisonParameterDefinition(2, BETWEEN, GREATER_THAN, LESS_THAN),
                dateTime1, dateTime2);
        this.geoIp2Util = geoIp2Util;
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        LocalDateTime usersDateTime = lookupDateTime(request);
        boolean evaluation;

        if(instance.comparison==BETWEEN) {
            evaluation = instance.comparison.perform(usersDateTime, instance.dateTime1, instance.dateTime2);
        } else {
            evaluation = instance.comparison.perform(usersDateTime, instance.dateTime1);
        }

        return evaluation;
    }

    private LocalDateTime lookupDateTime(HttpServletRequest request) {
        LocalDateTime localDateTime = null;
        InetAddress address;
        try {
            address = HttpRequestDataUtil.getIpAddress(request);
        } catch (UnknownHostException e) {
            throw new RuleEvaluationFailedException(e, "Unknown host.");
        }
        String ipAddress = address.getHostAddress();
        Calendar dateTime = null;
        try {
            dateTime = geoIp2Util.getDateTime(ipAddress);
        } catch (IOException | GeoIp2Exception e) {
            Logger.error(this, "Could not look up country for request. Using 'unknown': " + request.getRequestURL());
        }

        if(dateTime!=null) {
            localDateTime = LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.systemDefault());
        }
        return localDateTime;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(this, parameters);
    }

    public static class Instance implements RuleComponentInstance {

        public final LocalDateTime dateTime1;
        public final LocalDateTime dateTime2;
        public final Comparison<Comparable> comparison;

        public Instance(Conditionlet definition, Map<String, ParameterModel> parameters) {
            checkState(parameters != null && ((parameters.size() == 2 || parameters.size() == 3)), "Referring URL Condition requires either two (%s, %s) or three (%s, %s, %s) parameters.", COMPARISON_KEY, DATE_TIME_1_KEY, COMPARISON_KEY, DATE_TIME_1_KEY, DATE_TIME_2_KEY);

            assert parameters != null;

            String comparisonValue = parameters.get(COMPARISON_KEY).getValue();
            try {
                //noinspection unchecked
                this.comparison = ((ComparisonParameterDefinition) definition.getParameterDefinitions().get(COMPARISON_KEY)).comparisonFrom(comparisonValue);
            } catch (ComparisonNotPresentException e) {
                throw new ComparisonNotSupportedException("The comparison '%s' is not supported on Condition type '%s'",
                        comparisonValue,
                        definition.getId());
            }

            if(comparison==BETWEEN) {
                checkState(parameters.size() == 3, "DateTime Condition requires parameters %s, %s and %s.", COMPARISON_KEY, DATE_TIME_1_KEY, DATE_TIME_2_KEY);
                String value = parameters.get(DATE_TIME_2_KEY).getValue();
                this.dateTime2 = !Strings.isBlank(value)?LocalDateTime.parse(value):null;
            } else {
                checkState(parameters.size() >= 2, "DateTime Condition requires parameters %s and %s.", COMPARISON_KEY, DATE_TIME_1_KEY);
                this.dateTime2 = null;
            }

            String value = parameters.get(DATE_TIME_1_KEY).getValue();
            this.dateTime1 = !Strings.isBlank(value)?LocalDateTime.parse(value):null;
        }
    }
}
