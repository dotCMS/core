package com.dotcms.rest.api.v1.drive.export;

import java.nio.file.Path;

/**
 * A finished export file, waiting in a temp file to be sent.
 *
 * @param path        the temp file holding the complete export
 * @param fileName    the name the browser saves it under
 * @param mediaType   {@code text/csv; charset=UTF-8} or {@code application/zip}
 * @param contentlets the number of data rows written, over every CSV
 * @param files       the number of CSVs written
 */
public record ExportFile(Path path, String fileName, String mediaType, int contentlets, int files) {
}
