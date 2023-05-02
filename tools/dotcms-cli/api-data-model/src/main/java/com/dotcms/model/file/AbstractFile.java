package com.dotcms.model.file;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.folder.SimpleFolder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.nio.file.Paths;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = File.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractFile {

    String fileName();

    String status();

    String language();

    String site();

    SimpleFolder folder();

    default String getBundlePath() {
        return Paths.get("files",
                status(),
                language(),
                site(),
                folder().name(),
                fileName()).toString();
    }

}
