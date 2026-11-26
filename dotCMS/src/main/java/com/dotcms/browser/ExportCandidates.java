package com.dotcms.browser;

import java.util.List;

/**
 * The rows a Content Drive export resolved, before anything is written.
 *
 * @param rows      the readable rows, deduplicated by {@link ExportRow#key()}, in output order
 * @param overLimit {@code true} when more readable rows exist than the limit allowed; {@code rows}
 *                  is then incomplete and must not be written
 */
public record ExportCandidates(List<ExportRow> rows, boolean overLimit) {
}
