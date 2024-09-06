package com.dotcms.model.config;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.beans.Transient;
import java.util.Optional;
import java.util.function.Supplier;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = CredentialsBean.class)
public interface AbstractCredentialsBean {
     String user();

     /**
      * This method is used to retrieve the token from a secure store or a plain text store on demand
      * The attribute must be transient to avoid being serialized
      * @return a supplier that will return the token
      */
     @Nullable
     @Transient
     @Value.Auxiliary
     Supplier <char[]> tokenSupplier();

     /**
      * This method is used to retrieve the token from a secure store or a plain text store on demand
      * @return the token if present
      */
     @Transient
     @Value.Lazy
     default Optional<char[]> loadToken() {
          final Supplier<char[]> supplier = tokenSupplier();
          if (null != supplier) {
               final char[] chars = supplier.get();
               if (chars != null && chars.length > 0) {
                  return Optional.of(chars);
               }
          }
          return token();
     }

     /**
      * This is the token loaded in plain text from the configuration file
      * here's where the mapped will inject the value from the configuration file
      * @return the token if present
      */
     Optional<char[]> token();

}
