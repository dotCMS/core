package com.dotcms.uuid.shorty;

import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Optional;

/**
 * This implementation adds logic to handle clients
 * with legacy id to support shorty ids
 *
 * @author Will Ezell
 * @since Sep 20, 2016
 */
public class LegacyShortyIdAPIImpl extends ShortyIdAPIImpl {

    @Override
    public Optional<ShortyId> getShorty(final String shortStr, final ShortyInputType shortyType) {

        try {

            if (shortStr.startsWith(TempFileAPI.TEMP_RESOURCE_PREFIX) && APILocator.getTempFileAPI().isTempResource(shortStr)) {

                return Optional.of(new ShortyId(shortStr, shortStr, ShortType.TEMP_FILE, ShortType.TEMP_FILE));
            }

            final boolean isExactMatch = NumberUtils.isParsable(shortStr);  // check if it is or not a legacy id
            if (!isExactMatch) {

                validShorty(shortStr);
            }

            ShortyId shortyId;
            final Optional<ShortyId> opt = new ShortyIdCache().get(shortStr);
            if (opt.isPresent()) {
                shortyId = opt.get();
            } else if (shortStr.length() == 36 || isExactMatch) { // includes legacy here
                shortyId = viaDbEquals(shortStr, shortyType);
                new ShortyIdCache().add(shortyId);
            } else {
                shortyId = viaDbLike(shortStr, shortyType);
                new ShortyIdCache().add(shortyId);
            }
            return shortyId.type == ShortType.CACHE_MISS ? Optional.empty() : Optional.of(shortyId);
        } catch (final ShortyException se) {
            Logger.warn(this.getClass(), String.format("An error occurred when getting shorty value for '%s' of type " +
                    "'%s': %s", shortStr, shortyType, se.getMessage()));
            return Optional.empty();
        }
    }
}