package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import java.util.Date;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Provides the data context for rendering an {@link HTMLPageAsset}.
 *
 * @author Freddy Rodriguez
 * @since Feb 22nd, 2019
 */
@Value.Immutable
public abstract class PageContext {

    public abstract User getUser();

    public abstract PageMode getPageMode();

    @Nullable
    public abstract String getPageUri();

    @Nullable
    public abstract HTMLPageAsset getPage();

    @Value.Default
    public boolean isGraphQL() {
        return false;
    }

    @Value.Default
    public boolean isParseJSON() {
        return false;
    }

    @Nullable
    public abstract VanityURLView getVanityUrl();

    @Nullable
    public abstract String getPersona();

    @Nullable
    public abstract String getVariant();

    @Nullable
    public abstract Date getPublishDate();

    @Override
    public boolean equals(final Object another) {
        if (this == another) {
            return true;
        }
        if (another == null || getClass() != another.getClass()) {
            return false;
        }
        PageContext that = (PageContext) another;
        return Objects.equals(getUser(), that.getUser())
                && Objects.equals(getPageUri(), that.getPageUri())
                && getPageMode() == that.getPageMode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUser(), getPageUri(), getPageMode());
    }

}
