package com.dotcms.cdi;

import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
     * Get a bean from CDI container and return an Optional with the bean if found, empty otherwise
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
     * Get a bean from CDI container but throw an exception if the bean is not found
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

    /**
     * Get all beans of a given type from CDI container
     * @param clazz the class of the beans
     * @return a List containing all beans of the given type
     * @param <T> the type of the beans
     */
    public static <T> List<T> getBeans(Class<T> clazz) {
        try {
            return CDI.current().select(clazz).stream()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            String errorMessage = String.format("Unable to find beans of class [%s]: %s", clazz, e.getMessage());
            Logger.error(CDIUtils.class, errorMessage);
            throw new IllegalStateException(errorMessage, e);
        }
    }

}
