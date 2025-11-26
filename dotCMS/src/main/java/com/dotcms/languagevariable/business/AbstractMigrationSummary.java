package com.dotcms.languagevariable.business;

import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
public interface AbstractMigrationSummary {
    Map<Language, List<String>> success();
    Map<Language, List<String>> fails();

    List<Path> invalidFiles();

    List<Locale> nonExistingLanguages();

}
