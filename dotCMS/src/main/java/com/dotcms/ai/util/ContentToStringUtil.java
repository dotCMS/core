package com.dotcms.ai.util;


import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.viewtools.MarkdownTool;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockMap;
import com.dotcms.repackage.org.jsoup.Jsoup;
import com.dotcms.tika.TikaProxyService;
import com.dotcms.tika.TikaServiceBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.felix.framework.OSGISystem;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dotcms.ai.app.AppConfig.debugLogger;
import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.SPACE;


/**
 * This class is intended turn a contentlet into an "indexable" String.  It take a contentlet and
 * does its best guess as what field or fields
 * hold the important content and then returns them as a RAW string, stripped of any markup or HTML.
 * If the there are file fields on the content, and they are "indexable",  it will try to use
 * TIKA to extract the content from them
 */
public class ContentToStringUtil {

    public static final String FILE_ASSET_KEY = "fileAsset";
    public static final String ASSET_KEY = "asset";
    public static final Lazy<ContentToStringUtil> impl = Lazy.of(ContentToStringUtil::new);

    private static final String[] MARKDOWN_STRING_PATTERNS = {
            "(^|[\\n])\\s*1\\.\\s.*\\s+1\\.\\s",                    // markdown list with 1. \n 1.
            "(^|[\\n])\\s*-\\s.*\\s+-\\s",                          // markdown unordered list -
            "(^|[\\n])\\s*\\*\\s.*\\s+\\*\\s",                      // markdown unordered list *
            "\\s(__|\\*\\*)(?!\\s)(.(?!\\1))+(?!\\s(?=\\1))",       // markdown __bold__
            "\\[[^]]+\\]\\(https?:\\/\\/\\S+\\)",                   // markdown link [this is a link](http://linking)
            "\\n####\\s.*$",                                        // markdown h4
            "\\n###\\s.*$",                                         // markdown h3
            "\\n##\\s.*$",                                          // markdown h2
            "\\n#\\s.*$",                                           // markdown h1
            "\\n```"                                                // markdown code block


    };

    private static final Lazy<List<Pattern>> MARKDOWN_PATTERNS = Lazy.of(() -> Arrays.stream(MARKDOWN_STRING_PATTERNS).map(Pattern::compile).collect(Collectors.toUnmodifiableList()));

    private static final Pattern HTML_PATTERN = Pattern.compile(".*\\<[^>]+>.*");


    private final Lazy<TikaProxyService> tikaService = Lazy.of(() -> {
        try {
            return OSGISystem.getInstance().getService(TikaServiceBuilder.class, "com.dotcms.tika").createTikaService();
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    });

    private ContentToStringUtil() {

    }

    /**
     * This creates a tmp file with an .html extension so that tika can parse it as "html".  If tika fails, we fall
     * back to JSoup.  If that fails, we fall back to regex
     *
     * @param html
     * @return
     */
    public Optional<String> parseHTML(@NotNull String html) {

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(null, ".html");
            Files.write(tempFile, html.getBytes(StandardCharsets.UTF_8));
            return parseText(tikaService.get().parseToString(tempFile.toFile()));
        } catch (Exception e) {
            Logger.warnAndDebug(ContentToStringUtil.class, "Tika failed parsing, trying JSoup:" + e.getMessage(), e);
            try {
                return Optional.ofNullable(Jsoup.parse(html).text());
            } catch (Exception e1) {
                Logger.warnAndDebug(ContentToStringUtil.class, "JSoup failed parsing, trying regex:" + e1.getMessage(), e);
                return Try.of(() -> html.replaceAll("<[^>]*>", "")).toJavaOptional();
            }


        } finally {
            try {
                tempFile.toFile().delete();//NOSONAR
            } catch (Exception ex) {
                //NOSONAR
            }
        }
    }

    private Optional<String> parseText(@NotNull String val) {
        val = UtilMethods.isSet(val)
                ? val.replaceAll("\\s+", " ")
                : null;

        return Optional.ofNullable(val);
    }

    private Optional<String> parseBlockEditor(@NotNull String val) {

        final StoryBlockMap storyBlockMap = new StoryBlockMap(val);
        return parseHTML(storyBlockMap.toHtml());

    }

    private Optional<String> parseMarkdown(@NotNull String val) {

        try {
            return parseHTML(new MarkdownTool().parse(val));
        } catch (Throwable e) {
            throw new DotRuntimeException(e);
        }

    }

    public Optional<String> turnContentletIntoString(@NotNull Contentlet contentlet) {

        return parseFields(contentlet, guessWhatFieldsToIndex(contentlet));
    }

    /**
     * This method will index the first long_text field that has been marked as indexed
     *
     * @param contentlet The {@link Contentlet} whose fields will be analyzed.
     *
     * @return A list of fields to index.
     */
    public List<Field> guessWhatFieldsToIndex(@NotNull Contentlet contentlet) {
        // HTML Pages are not indexed based on fields, instead they will be rendered to be parsed
        if (Boolean.TRUE.equals(contentlet.isHTMLPage())) {
            return List.of();
        }
        final List<Field> embedMe = new ArrayList<>();
        if (contentlet.isFileAsset()) {
            final File fileAsset = Try.of(() -> contentlet.getBinary(FILE_ASSET_KEY)).getOrNull();
            if (shouldIndex(fileAsset)) {
                embedMe.add(contentlet.getContentType().fieldMap().get(FILE_ASSET_KEY));
            }
        }
        if (contentlet.isDotAsset()) {
            final File asset = Try.of(() -> contentlet.getBinary(ASSET_KEY)).getOrNull();
            if (shouldIndex(asset)) {
                embedMe.add(contentlet.getContentType().fieldMap().get(ASSET_KEY));
            }
        }
        if (!embedMe.isEmpty()) {
            return embedMe;
        }
        final String ignoreUrlMapFields = (contentlet.getContentType().urlMapPattern() != null) ? contentlet.getContentType().urlMapPattern() : BLANK;

        contentlet.getContentType()
                .fields()
                .stream().filter(f -> null != ignoreUrlMapFields && !ignoreUrlMapFields.contains("{" + f.variable() + "}"))
                .filter(f -> f instanceof StoryBlockField || f instanceof WysiwygField || f instanceof BinaryField ||  f instanceof TextAreaField || f instanceof FileField);

        final List<Field> indexableFields = contentlet.getContentType()
                .fields()
                .stream().filter(f -> null != ignoreUrlMapFields && !ignoreUrlMapFields.contains("{" + f.variable() + "}"))
                .filter(f ->
                        f.dataType().equals(DataTypes.LONG_TEXT)
                ).collect(Collectors.toUnmodifiableList());
        debugLogger(this.getClass(), () -> String.format("Found %d indexable field(s) for Contentlet ID '%s': %s",
                indexableFields.size(), contentlet.getIdentifier(), indexableFields.stream().map(Field::variable).collect(Collectors.toSet())));
        return indexableFields;
    }

    private boolean shouldIndex(File file) {

        final Set<String> indexFileExtensions = Set.of(ConfigService.INSTANCE.config().getConfigArray(AppKeys.EMBEDDINGS_FILE_EXTENSIONS_TO_EMBED));
        final int minimumFileLength = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_MINIMUM_FILE_SIZE_TO_INDEX);


        return file != null
                && file.exists()
                && indexFileExtensions.contains(UtilMethods.getFileExtension(file.toString()))
                && file.length() > minimumFileLength;
    }


    private Optional<String> parseFile(@NotNull File file) {
        if (!shouldIndex(file)) {
            return Optional.empty();
        }

        return Try.of(() -> tikaService.get().parseToString(file)).toJavaOptional();

    }

    /**
     * Takes the actual content (data) from the specified list of Fields in a Contentlet, and
     * appends it to a single String. This will make up the information that will be sent to the
     * Embeddings API.
     * <p>Keep in mind that the length of such data must be greater or equal than the value
     * specified via the {@link AppKeys#EMBEDDINGS_MINIMUM_TEXT_LENGTH_TO_INDEX} configuration
     * property. Otherwise, an empty Optional will be returned.</p>
     *
     * @param contentlet The {@link Contentlet} containing the data that will be sent to the
     *                   Embeddings API.
     * @param fields     The list of {@link Field} objects specifying what data will be taken from
     *                   the Contentlet.
     *
     * @return An {@link Optional} with the appended Strings from each field value.
     */
    public Optional<String>     parseFields(@NotNull final Contentlet contentlet, @NotNull final List<Field> fields) {
        if (UtilMethods.isEmpty(contentlet::getIdentifier)) {
            return Optional.empty();
        }

        // if no field is specified and it is an html page, send it
        if (fields.isEmpty() && contentlet.isHTMLPage() == Boolean.TRUE) {
            return parsePage(contentlet);
        }
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        final StringBuilder builder = new StringBuilder();
        for (Field field : fields) {
            parseField(contentlet, field)
                    .ifPresent(s -> builder.append(s).append(SPACE));
        }
        final int embeddingsMinimumLength =
                ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_MINIMUM_TEXT_LENGTH_TO_INDEX);
        if (builder.length() < embeddingsMinimumLength) {
            debugLogger(this.getClass(), () -> String.format("Parseable fields for Contentlet ID " +
                    "'%s' don't meet the minimum length requirement of %d characters. Skipping indexing.",
                    contentlet.getIdentifier(), embeddingsMinimumLength));
            return Optional.empty();
        }

        return Optional.of(builder.toString().trim());
    }

    /**
     * Extracts the contents of a specific field from a Contentlet. Keep in mind that, depending on
     * the format of such a content or how it was saved, the method will try to parse it based on
     * specific strategies/ways to retrieve or interpret it.
     *
     * @param contentlet The {@link Contentlet} that the field's value will be extracted from.
     * @param field      The {@link Field} whose content will be extracted.
     *
     * @return An {@link Optional} with the extracted content, if any.
     */
    private Optional<String> parseField(@NotNull final Contentlet contentlet, @NotNull final Field field) {
        final ContentType type = contentlet.getContentType();
        if (field instanceof BinaryField) {
            Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is a BinaryField ");
            return parseFile(Try.of(() -> contentlet.getBinary(field.variable())).getOrNull());
        }
        final String value = contentlet.getStringProperty(field.variable());

        if(UtilMethods.isEmpty(value)){
            return Optional.empty();
        }

        // handle attached files
        if (field instanceof FileField) {
            Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is a FileField ");
            return handleFileField(contentlet,value);
        }
        if (field instanceof StoryBlockField && StringUtils.isJson(value)) {
            Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is a StoryBlockField ");
            return parseBlockEditor(value);
        }
        if (isMarkdown(value)) {
            Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is a markdown field");
            return parseMarkdown(value);
        }
        if (isHtml(value)) {
            Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is an HTML field");
            return parseHTML(value);
        }
        Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is a " +
                (field instanceof CustomField ? "Custom Field" : " text field"));
        return parseText(value);
    }

    private Optional<String> handleFileField(Contentlet contentlet, String identifier) {
        Optional<Contentlet> con = APILocator.getContentletAPI().findContentletByIdentifierOrFallback(identifier,false,contentlet.getLanguageId(),APILocator.systemUser(),false);
        if(con.isEmpty()) {
            return Optional.empty();
        }
        if(con.get().isDotAsset()){
            return parseFile(Try.of(() -> con.get().getBinary(ASSET_KEY)).getOrNull());
        }
        else if(con.get().isFileAsset()){
            return parseFile(Try.of(() -> con.get().getBinary(FILE_ASSET_KEY)).getOrNull());
        }
        return Optional.empty();
    }


    private Optional<String> parsePage(Contentlet pageContentlet) {
        if (UtilMethods.isEmpty(pageContentlet::getIdentifier)) {
            return Optional.empty();
        }
        try {
            if (Boolean.FALSE.equals(pageContentlet.isHTMLPage())) {
                return Optional.empty();
            }
            String pageHTML = APILocator.getHTMLPageAssetAPI().getHTML(APILocator.getHTMLPageAssetAPI().fromContentlet(pageContentlet), true, null, APILocator.systemUser(), "dot-user-agent");

            return parseHTML(pageHTML);
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "parsePage:" + pageContentlet + " failed:" + e.getMessage(), e);
            return Optional.empty();
        }
    }

    public boolean isMarkdown(@NotNull String value) {
        if (UtilMethods.isEmpty(value)) {
            return false;
        }

        String converted = Try.of(() -> new MarkdownTool().parse(value.substring(0, Math.min(value.length(), 10000)))).getOrNull();
        if (converted == null || value.equals(converted)) {
            return false;
        }
        // its markdown if we get two or more matching patterns
        return MARKDOWN_PATTERNS.get().stream().filter(p -> p.matcher(value).find()).count() > 1;
    }

    // it is HTML if parsing it returns a different value than it
    public boolean isHtml(@NotNull String value) {
        if (UtilMethods.isEmpty(value)) {
            return false;
        }
        return HTML_PATTERN.matcher(value).find();


    }


}
