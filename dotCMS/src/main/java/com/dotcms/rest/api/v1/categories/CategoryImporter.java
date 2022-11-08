package com.dotcms.rest.api.v1.categories;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.util.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CategoryImporter {


    public static List<CategoryDTO> from(final BufferedReader bufferedReader) throws IOException {
        List<CategoryDTO> result = new ArrayList<>();

        CsvReader csvreader = null;
        try {
            csvreader = new CsvReader(bufferedReader);
            csvreader.setSafetySwitch(false);
            csvreader.readHeaders();
            String[] csvLine;

            while (csvreader.readRecord()) {
                csvLine = csvreader.getValues();
                try {
                    result.add(new CategoryDTO(csvLine[0], csvLine[2],
                            csvLine[1], null, csvLine[3]));

                } catch (Exception e) {
                    Logger.error(CategoryImporter.class,
                            "Error trying to parse the categories csv row: name=" + csvLine[0]
                                    + ", variable=" + csvLine[2] + ", key=" + csvLine[1] + ", sort="
                                    + csvLine[3], e);
                }
            }
        } finally {
            CloseUtils.closeQuietly(bufferedReader);
            csvreader.close();
        }

        return result;
    }
}
