package com.dotcms.rest.api.v1.categories;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.util.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CategoryImporter {

    static final String[] REQUIRED_HEADERS = {"name", "key", "variable", "sort"};

    public static List<CategoryDTO> from(final BufferedReader bufferedReader) throws IOException {
        List<CategoryDTO> result = new ArrayList<>();

        CsvReader csvreader = null;
        try {
            csvreader = new CsvReader(bufferedReader);
            csvreader.setSafetySwitch(false);
            csvreader.readHeaders();

            if (!hasRequiredHeaders(csvreader)) {
                throw new IOException(
                        "Invalid CSV format: missing required columns (name, key, variable, sort)");
            }

            String[] csvLine;

            while (csvreader.readRecord()) {
                csvLine = csvreader.getValues();
                try {
                    result.add(new CategoryDTO(csvLine[0].trim(), csvLine[2].trim(),
                            csvLine[1].trim(), null, csvLine[3].trim()));

                } catch (Exception e) {
                    Logger.error(CategoryImporter.class,
                            "Error trying to parse the categories csv row: name=" + csvLine[0]
                                    + ", variable=" + csvLine[2] + ", key=" + csvLine[1] + ", sort="
                                    + csvLine[3], e);
                }
            }
        } finally {
            CloseUtils.closeQuietly(bufferedReader);
            if (csvreader != null) {
                csvreader.close();
            }
        }

        return result;
    }

    private static boolean hasRequiredHeaders(final CsvReader csvreader) throws IOException {
        final String[] headers = csvreader.getHeaders();
        if (headers == null || headers.length < REQUIRED_HEADERS.length) {
            return false;
        }
        for (int i = 0; i < REQUIRED_HEADERS.length; i++) {
            if (!REQUIRED_HEADERS[i].equalsIgnoreCase(headers[i].trim())) {
                return false;
            }
        }
        return true;
    }
}
