package com.dotcms.rest.api.v1.drive.export;

import com.dotmarketing.util.UtilMethods;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Names the files of a Content Drive export and builds their {@code Content-Disposition} header.
 * <p>
 * CSVs are named as the Content Search export names them, {@code <ContentType>_contents_<M_d_yyyy>.csv};
 * a zip is named {@code <host>_export_<M_d_yyyy>.zip}. Names are made safe for a file system, a zip
 * entry and an HTTP header, and are kept distinct within one zip.
 */
public final class ExportFileNames {

    /** The date pattern Content Search uses in its file name. */
    public static final String DATE_PATTERN = "M_d_yyyy";

    /** Characters a file name, a zip entry or a header must not hold: path and shell separators and every control character. */
    private static final Pattern UNSAFE = Pattern.compile("[/\\\\:*?\"<>|\\p{Cntrl}]");
    /** Dots and spaces at either end, which some file systems drop or reject. */
    private static final Pattern EDGE_DOTS_AND_SPACES = Pattern.compile("^[.\\s]+|[.\\s]+$");
    /** The characters RFC 5987 lets through unencoded in {@code filename*}, besides letters and digits. */
    private static final String ATTR_CHARS = "!#$&+-.^_`|~";

    private static final String CSV_EXTENSION = ".csv";

    private ExportFileNames() {
    }

    /**
     * The name of one content type's CSV.
     *
     * @param contentTypeName     the content type's name, which the file is named after
     * @param contentTypeVariable the content type's variable, used when the name has nothing safe left
     * @param date                the export date
     * @return {@code <safe name>_contents_<M_d_yyyy>.csv}
     */
    public static String csvName(final String contentTypeName, final String contentTypeVariable,
            final Date date) {
        String base = safe(contentTypeName);
        if (base.isEmpty()) {
            base = safe(contentTypeVariable);
        }
        return base + "_contents_" + formatDate(date) + CSV_EXTENSION;
    }

    /**
     * The name of a selection-mode zip.
     *
     * @param hostname the name of the site being browsed
     * @param date     the export date
     * @return {@code <safe hostname>_export_<M_d_yyyy>.zip}
     */
    public static String zipName(final String hostname, final Date date) {
        return safe(hostname) + "_export_" + formatDate(date) + ".zip";
    }

    /**
     * Names the CSVs of one zip. Content types are named in content type name order, and a name
     * already taken gets {@code -2}, {@code -3}, and so on before {@code .csv}. Names are compared
     * ignoring case, since the zip may be extracted on a file system that does.
     *
     * @param types the content types in the zip
     * @param date  the export date
     * @return the file name of each content type, keyed by its id, in the order the entries are written
     */
    public static Map<String, String> csvNames(final List<TypeName> types, final Date date) {
        final List<TypeName> sorted = new ArrayList<>(types);
        sorted.sort(Comparator.comparing((TypeName type) -> String.valueOf(type.name()),
                String.CASE_INSENSITIVE_ORDER));

        final Map<String, String> names = new LinkedHashMap<>();
        final Set<String> taken = new HashSet<>();
        for (final TypeName type : sorted) {
            final String plain = csvName(type.name(), type.variable(), date);
            String name = plain;
            final String stem = plain.substring(0, plain.length() - CSV_EXTENSION.length());
            for (int suffix = 2; !taken.add(name.toLowerCase(Locale.ROOT)); suffix++) {
                name = stem + "-" + suffix + CSV_EXTENSION;
            }
            names.put(type.id(), name);
        }
        return names;
    }

    /**
     * Builds the {@code Content-Disposition} value for a download: an ASCII {@code filename} for old
     * clients and the exact name, percent-encoded, in {@code filename*}.
     *
     * @param fileName the file name, already made safe
     * @return {@code attachment; filename="..."; filename*=UTF-8''...}
     */
    public static String contentDisposition(final String fileName) {
        final StringBuilder ascii = new StringBuilder(fileName.length());
        fileName.codePoints().forEach(codePoint -> ascii.append(
                codePoint < 0x20 || codePoint > 0x7E || codePoint == '"' || codePoint == '\\'
                        ? "_" : Character.toString(codePoint)));
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encode(fileName);
    }

    /**
     * Percent-encodes a value for an RFC 5987 extended parameter: every UTF-8 byte that is not a
     * letter, a digit or an attribute character becomes {@code %XX}; a space is {@code %20}, never
     * {@code +}.
     *
     * @param value the value
     * @return the encoded value
     */
    private static String encode(final String value) {
        final StringBuilder encoded = new StringBuilder();
        for (final byte b : value.getBytes(StandardCharsets.UTF_8)) {
            final char c = (char) (b & 0xFF);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || ATTR_CHARS.indexOf(c) >= 0) {
                encoded.append(c);
            } else {
                encoded.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return encoded.toString();
    }

    /**
     * Makes a name safe: every unsafe character becomes {@code _}, then dots and spaces are trimmed
     * from both ends. Letters of any script are kept.
     *
     * @param name the name, possibly {@code null}
     * @return the safe name, possibly empty
     */
    private static String safe(final String name) {
        if (name == null) {
            return "";
        }
        final String replaced = UNSAFE.matcher(name).replaceAll("_");
        return EDGE_DOTS_AND_SPACES.matcher(replaced).replaceAll("");
    }

    /**
     * Formats the export date as Content Search does.
     *
     * @param date the export date
     * @return the date as {@code M_d_yyyy}
     */
    private static String formatDate(final Date date) {
        return UtilMethods.dateToHTMLDate(date, DATE_PATTERN);
    }

    /**
     * The parts of a content type that its CSV name is built from.
     *
     * @param id       the content type id
     * @param name     the content type name
     * @param variable the content type variable
     */
    public record TypeName(String id, String name, String variable) {
    }
}
