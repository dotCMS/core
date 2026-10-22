package com.dotcms.saml.service.impl;

import com.dotcms.saml.service.InvalidateAware;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to parse HTML without using velocity
 * @author jsanca
 */
public class RawTemplateProcessor implements InvalidateAware {

    private final Map<String, String> templateContentMap = new ConcurrentHashMap<>();

    private static class SingletonHolder {
        private static final RawTemplateProcessor INSTANCE = new RawTemplateProcessor();
    }

    public static RawTemplateProcessor getInstance() {
        return RawTemplateProcessor.SingletonHolder.INSTANCE;
    }

    protected RawTemplateProcessor() {
    }

    @Override
    public void invalidate() {

        templateContentMap.clear();
    }

    public void renderTemplateFile (final Writer out, final Map<String, String> contextMap, final String templateFile) throws IOException {

        this.renderTemplateContent(out, contextMap, this.getTemplateContent(templateFile));
    }

    private String getTemplateContent (final String templateFile) throws IOException {

        if (!this.templateContentMap.containsKey(templateFile)) {

            final StringWriter stringWriter = new StringWriter();
            try (final InputStream in = this.getClass().getResourceAsStream(templateFile )) {

                IOUtils.copy(in, stringWriter, "UTF-8");
            }
            this.templateContentMap.put(templateFile, stringWriter.toString());
        }

        return this.templateContentMap.get(templateFile);
    }

    public void renderTemplateContent (final Writer out, final Map<String, String> contextMap, final String templateContent) throws IOException {

        String parserContent = templateContent;
        for (final Map.Entry<String, String> contextEntry : contextMap.entrySet()) {

            parserContent = StringUtils.replace(parserContent, this.wrapKey(contextEntry.getKey()), contextEntry.getValue());
        }

        out.write(parserContent);
    }

    private String wrapKey(final String key) {

        return "${" + key + "}";
    }
}
