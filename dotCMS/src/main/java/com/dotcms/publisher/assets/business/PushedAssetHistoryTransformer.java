package com.dotcms.publisher.assets.business;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.content.PushedAssetHistory;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.liferay.util.StringPool.BLANK;

/**
 * This transformer converts the raw data of pushed asset history coming from the database into a
 * hydrated Java object.
 *
 * @author Jose Castro
 * @since Sep 30th, 2025
 */
public class PushedAssetHistoryTransformer implements DBTransformer<PushedAssetHistory> {

    final List<PushedAssetHistory> list;

    /**
     * Traverses the records coming from the database and transforms each of them into objects of
     * type {@link PushedAssetHistory}.
     *
     * @param initList The list of records coming from the database.
     */
    public PushedAssetHistoryTransformer(final List<Map<String, Object>> initList){
        final List<PushedAssetHistory> newList = new ArrayList<>();
        if (UtilMethods.isSet(initList)) {
            initList.forEach(map -> newList.add(transform(map)));
        }
        this.list = newList;
    }

    @Override
    public List<PushedAssetHistory> asList() {
        return UtilMethods.isSet(this.list) ? this.list : List.of();
    }

    @Override
    public PushedAssetHistory findFirst() {
        return DBTransformer.super.findFirst();
    }

    /**
     * Transforms the raw data coming from the database into a hydrated Java object with the
     * appropriate Pushed Asset History data.
     *
     * @param map The raw data from the database.
     *
     * @return The {@link PushedAssetHistory} object.
     */
    private PushedAssetHistory transform(final Map<String, Object> map)  {
        final String environment = map.get("environment_name").toString();
        final Date pushDate = (Date) map.get("push_date");
        final String bundleId = map.get("bundle_id").toString();
        final String owner = map.get("owner").toString();
        String pushedBy = BLANK;
        if (UtilMethods.isSet(bundleId)) {
            try {
                final User ownerUser = APILocator.getUserAPI().loadUserById(map.get("owner").toString());
                pushedBy = ownerUser.getFullName();
            } catch (final DotDataException | DotSecurityException e) {
                Logger.warn(this, String.format("Failed to return User with ID '%s': %s", owner,
                        ExceptionUtil.getErrorMessage(e)));
            } catch (final NoSuchUserException e) {
                pushedBy = Try.of(() -> LanguageUtil.get("Deleted")).getOrElse("Deleted");
            }
        }
        return new PushedAssetHistory(pushedBy, environment, pushDate, bundleId);
    }

}
