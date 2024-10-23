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
                return Optional.of(getBeanThrows(clazz));
            } catch (Exception e) {
                // Exception is already logged in getBeanThrows
            }
            return Optional.empty();
    }


    /**
     * Get a bean from CDI container
     * @param clazz the class of the bean
     * @return the bean
     * @param <T> the type of the bean
     */
    public static <T> T getBeanThrows(Class<T> clazz) {
        try {
            return CDI.current().select(clazz).get();
        } catch (Exception e) {
            String errorMessage = String.format("Unable to find bean of class [%s]: %s", clazz, e.getMessage());
            Logger.error(CDIUtils.class, errorMessage);
            throw new IllegalStateException(errorMessage, e);
        }
    }

}
