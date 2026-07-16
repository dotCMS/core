package com.dotcms.telemetry.collectors.theme;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Theme;
import com.dotmarketing.business.ThemeAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

import javax.ws.rs.NotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class provides the basic data set for other Theme-related Metrics to work on. In this case,
 * all the {@link Theme} objects in the dotCMS repository; i.e., all Themes from all Sites.
 *
 * @author Jose Castro
 * @since Mar 25th, 2025
 */
public abstract class ThemeDataMetricType implements MetricType {

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.LAYOUT;
    }

    @Override
    public Optional<MetricValue> getStat() throws DotDataException {
        final ThemeAPI themeAPI = APILocator.getThemeAPI();
        List<Theme> themes = new ArrayList<>();
        try {
            themes = themeAPI.findThemes("", APILocator.systemUser(), 0, 0, "",
                    OrderDirection.ASC, "", false);
        } catch (final DotSecurityException e) {
            Logger.warn(this, String.format("An error occurred when retrieving the Themes: %s",
                    ExceptionUtil.getErrorMessage(e)));
        }
        return getValue(themes)
                .map(o -> new MetricValue(this.getMetric(), o))
                .or(() -> Optional.of(new MetricValue(this.getMetric(), 0)));
    }

    @Override
    public Optional<Object> getValue() {
        throw new NotSupportedException("This method must be implemented by inherited classes");
    }

    /**
     * This method is intended to be overridden by the concrete classes that extend this class. The
     * main goal is to have other Metric classes provide specific information related to dotCMS
     * Themes.
     *
     * @param themes The list of all {@link Theme} objects in the dotCMS repository.
     *
     * @return An {@link Optional} object representing the information provided by the implementing
     * Metric class.
     *
     * @throws DotDataException An error occurred when retrieving the data from the dotCMS
     *                          repository.
     */
    abstract Optional<Object> getValue(final List<Theme> themes) throws DotDataException;

}
