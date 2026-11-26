package com.dotcms.rest.api.v1.drive.export;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The name of a selection-mode zip.
     *
     * @param hostname the name of the site being browsed
     * @param date     the export date
     * @return {@code <safe hostname>_export_<M_d_yyyy>.zip}
     */
    public static String zipName(final String hostname, final Date date) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Names the CSVs of one zip. Content types are named in content type name order, and a name
     * already taken gets {@code -2}, {@code -3}, and so on before {@code .csv}.
     *
     * @param types the content types in the zip
     * @param date  the export date
     * @return the file name of each content type, keyed by its id, in the order the entries are written
     */
    public static Map<String, String> csvNames(final List<TypeName> types, final Date date) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Builds the {@code Content-Disposition} value for a download: an ASCII {@code filename} for old
     * clients and the exact name, percent-encoded, in {@code filename*}.
     *
     * @param fileName the file name, already made safe
     * @return {@code attachment; filename="..."; filename*=UTF-8''...}
     */
    public static String contentDisposition(final String fileName) {
        throw new UnsupportedOperationException("not implemented");
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
