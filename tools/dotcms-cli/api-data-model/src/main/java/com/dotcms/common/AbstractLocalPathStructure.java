package com.dotcms.common;

import com.dotcms.model.annotation.ValueType;
import java.io.File;
import java.nio.file.Path;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;

/**
 * Represents the structure of a local file or directory path within the workspace.
 */
@ValueType
@Value.Immutable
public interface AbstractLocalPathStructure {
    boolean isDirectory();
    String status();
    String language();
    String site();
    @Nullable
    String fileName();
    String folderPath();
    Path filePath();
    @Default
    default boolean languageExists() {return false;}

    /**
     * Determines if the language of the path structure is the default language.
     *
     * @return true if the language of the path structure is the default language, false otherwise
     */
    @Default
    default boolean isDefaultLanguage() {
        return false;
    }

    @Derived
    default String folderName() {

        final int nameCount = filePath().getNameCount();

        String folderName = File.separator;

        if (nameCount > 1) {
            folderName = filePath().subpath(nameCount - 1, nameCount).toString();
        } else if (nameCount == 1) {
            folderName = filePath().subpath(0, nameCount).toString();
        }

        if (folderName.equalsIgnoreCase(this.site())) {
            folderName = File.separator;
        }

        return folderName;
    }

}
