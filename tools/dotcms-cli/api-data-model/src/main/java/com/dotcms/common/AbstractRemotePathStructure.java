package com.dotcms.common;

import com.dotcms.model.annotation.ValueType;
import java.nio.file.Path;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;

/**
 * Represents the site, folder path and file name components of the parsed remote path.
 */
@ValueType
@Value.Immutable
public interface AbstractRemotePathStructure {

    /**
     * The site component of the parsed path.
     */
    String site();

    /**
     * The folder path component of the parsed path.
     */
    Path folderPath();

    /**
     * The file name component of the parsed path.
     */
    @Nullable
    String fileName();

    @Derived
    default String folderName() {

        int nameCount = folderPath().getNameCount();

        String folderName = "/";

        if (nameCount > 1) {
            folderName = folderPath().subpath(nameCount - 1, nameCount).toString();
        } else if (nameCount == 1) {
            folderName = folderPath().subpath(0, nameCount).toString();
        }

        return folderName;
    }


    class Builder extends RemotePathStructure.Builder {
        public Builder folder(Path path) {
            super.fileName(null);
            super.folderPath(path);
            return this;
        }

        public Builder asset(Path path) {
            super.fileName(path.getFileName().toString());
            super.folderPath(path.getParent());
            return this;
        }

    }

    static Builder builder() {
        return new Builder();
    }

}
