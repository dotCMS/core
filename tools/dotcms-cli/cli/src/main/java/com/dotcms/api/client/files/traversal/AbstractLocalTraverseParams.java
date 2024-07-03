package com.dotcms.api.client.files.traversal;

import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.annotation.ValueType;
import java.io.File;
import java.io.Serializable;
import org.immutables.value.Value.Default;
import org.jboss.logging.Logger;
import org.immutables.value.Value;
import jakarta.annotation.Nullable;

/**
 * Just a class to compile all the params shared by various Traverse APIs
 */
@ValueType
@Value.Immutable
public interface AbstractLocalTraverseParams extends Serializable {

    @Nullable
    OutputOptionMixin output();
    @Nullable
    Logger logger();
    @Nullable
    Retriever retriever();
    @Default
    default boolean siteExists() {return false;}
    String sourcePath();
    File workspace();
    boolean removeAssets();
    boolean removeFolders();
    boolean ignoreEmptyFolders();
    boolean failFast();

}
