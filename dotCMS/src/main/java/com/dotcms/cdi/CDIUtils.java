package com.dotcms.cdi;


import com.dotmarketing.util.Logger;
import java.util.Optional;
import javax.enterprise.inject.spi.CDI;

public class CDIUtils {

    private CDIUtils() {
    }

    public static <T> Optional<T> getBean(Class<T> clazz) {
           try {
               return Optional.of(CDI.current().select(clazz).get());
           } catch (Exception e) {
               Logger.warn(CDIUtils.class, String.format("Unable to find bean of class [%s]",clazz), e);
               return Optional.empty();
           }
    }


}
