package com.dotcms.model.folder;

import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.file.File;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = Folder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractFolder {

    String parent();

    String name();

    int level();

    List<Folder> subFolders();

    List<File> files();
}
