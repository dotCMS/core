package com.dotcms.cdi;

import com.dotmarketing.util.Logger;
import java.util.Optional;
import javax.enterprise.inject.spi.CDI;

/**
 * Utility class to get beans from CDI container
 */
public class CDIUtils {

    /**
     * Private constructor to avoid instantiation
     */
    private CDIUtils() {
    }

    /**
     * Get a bean from CDI container
     * @param clazz the class of the bean
     * @return an Optional with the bean if found, empty otherwise
     */
    public static <T> Optional<T> getBean(Class<T> clazz) {
        try {
            return Optional.of(CDI.current().select(clazz).get());
        } catch (Exception e) {
            Logger.error(CDIUtils.class,
                String.format("Unable to find bean of class [%s] [%s]", clazz, e.getMessage())
            );
        }
        return Optional.empty();
    }


}
