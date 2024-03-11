package com.dotmarketing.startup.runonce;

import static com.dotcms.languagevariable.business.LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class Task240306MigrateLegacyLanguageVariables implements StartupTask {

    Path messagesDir() {
        return Paths.get(ConfigUtils.getDynamicContentPath(),"messages");
     }
    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            final Path path = messagesDir();
            migrateLegacyLanguageVariables(path);
        } catch (Exception e) {
            throw new DotRuntimeException("Error migrating legacy language variables", e);
        }
    }

    Lazy<ContentType> langVarContentType = Lazy.of(() -> {
        ContentType contentType = Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser()).find(LANGUAGEVARIABLE_VAR_NAME)).getOrNull();
        if (contentType == null) {
            throw new DotRuntimeException("Language variable content type not found");
        }
        return contentType;
    });

    @CanIgnoreReturnValue
    boolean migrateLegacyLanguageVariables(final Path messagesDir) throws IOException {
        if (!Files.exists(messagesDir)) {
            Logger.info(this," No messages directory found. Skipping language variable migration.");
            return false;
        }
        try (Stream<Path> files = Files.list(messagesDir).filter(path -> path.toString().endsWith(".properties")).sorted(Comparator.comparing(o -> o.getFileName().toString()))){
            files.forEach(path -> {
                // Do stuff
                final String fileName = path.getFileName().toString();
                final String cmsLanguage = fileName.replace("cms_language_", "").replace(".properties", "");
                Logger.info(this," Extracted language code: " + cmsLanguage);
                final Locale locale = LanguageUtil.validateLanguageTag(cmsLanguage);
                final long languageId = APILocator.getLanguageAPI().getLanguage(locale.getLanguage(),locale.getCountry()).getId();
                Logger.info(this," Extracted language id: " + languageId);
            });
        }
        return true;
    }

    Contentlet saveLanguageVariableContent(final String key, final String value, final long languageId) {
        final ContentType contentType = langVarContentType.get();
        final Contentlet langVar = new Contentlet();
        langVar.setContentTypeId(contentType.id());
        langVar.setLanguageId(languageId);
        langVar.setIndexPolicy(IndexPolicy.FORCE);
        langVar.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        langVar.setStringProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR, key);
        langVar.setStringProperty(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR, value);
        return langVar;
    }

    Map<Locale, List<Locale>> languageVariantMap = new HashMap<>();

}
