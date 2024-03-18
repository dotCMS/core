package com.dotcms.model.config;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.beans.Transient;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
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
     Supplier <char[]> tokenSupplier();

     @Value.Derived
     default Optional<char[]> token() {
          final Supplier<char[]> supplier = tokenSupplier();
          if (null != supplier){
               try {
                    final char[] chars = supplier.get();
                    if (chars != null && chars.length > 0) {
                         return Optional.of(chars);
                    }
               } catch (Exception e) {
                    //ignore
                    e.printStackTrace();
               }
          }
          return Optional.empty();
     }

}
