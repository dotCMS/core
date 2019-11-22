package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.dateFormat;
import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.datetimeFormat;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @author nollymar
 */
public class LuceneQueryDateTimeFormatter {


    //Date formats supported by dotCMS in lucene queries
    private static final Map<String, String> LUCENE_DATE_TIME_FORMAT_PATTERN = new LinkedHashMap<String, String>(){
        {
            put("MM/dd/yyyy hh:mm:ssa",
                    "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?");

            put("MM/dd/yyyy hh:mm:ss a",
                    "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?");

            put("MM/dd/yyyy hh:mm a",
                    "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?");

            put("MM/dd/yyyy hh:mma",
                    "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?");

            put("MM/dd/yyyy HH:mm:ss",
                    "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2})\\\"?");

            put("MM/dd/yyyy HH:mm",
                    "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{1,2})\\\"?");

            put(datetimeFormat.getPattern(),
                    "\\\"?(\\d{4}\\d{2}\\d{2}\\d{2}\\d{2}\\d{2})\\\"?");

            put(dateFormat.getPattern(), "\\\"?(\\d{4}\\d{2}\\d{2})\\\"?");

            put("MM/dd/yyyy", "\\\"?(\\d{1,2}/\\d{1,2}/\\d{4})\\\"?");

            put("hh:mm:ssa", "\\\"?(\\d{1,2}:\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?");

            put("hh:mm:ss a", "\\\"?(\\d{1,2}:\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?");

            put("HH:mm:ss", "\\\"?(\\d{1,2}:\\d{1,2}:\\d{1,2})\\\"?");

            put("hh:mma", "\\\"?(\\d{1,2}:\\d{1,2}(?:AM|PM|am|pm))\\\"?");

            put("hh:mm a", "\\\"?(\\d{1,2}:\\d{1,2}\\s+(?:AM|PM|am|pm))\\\"?");

            put("HH:mm", "\\\"?(\\d{1,2}:\\d{1,2})\\\"?");
        }
    };

    private static final List<String> LUCENE_TIME_FORMAT_PATTERNS = new ArrayList<>(
            Arrays.asList("hh:mm:ssa", "hh:mm:ss a", "HH:mm:ss", "hh:mma", "hh:mm a", "HH:mm"));
    /**
     *
     * @param query
     * @return
     */
    static String findAndReplaceQueryDates(String query) {
        query = RegEX.replaceAll(query, " ", "\\s{2,}");

        //delete additional blank spaces on date range
        if(UtilMethods.contains(query, "[ ")) {
            query = query.replace("[ ", "[");
        }

        if(UtilMethods.contains(query, " ]")) {
            query = query.replace(" ]", "]");
        }

        String clausesStr = RegEX.replaceAll(query, "", "[\\+\\-\\(\\)]*");
        String[] tokens = clausesStr.split(" ");
        String token;
        List<String> clauses = new ArrayList<String>();
        for (int pos = 0; pos < tokens.length; ++pos) {
            token = tokens[pos];
            if (token.matches("\\S+\\.\\S+:\\S*")) {
                clauses.add(token);
            } else if (token.matches("\\d+:\\S*")) {
                clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + " " + token);
            } else if (token.matches("\\[\\S*")) {
                clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + token);
            } else if (token.matches("TO") || token.toLowerCase().matches("am") || token.toLowerCase().matches("pm")) {
                clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + " " + token);
            } else if (token.matches("\\S*\\]")) {
                clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + " " + token);
            } else if (token.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                clauses.set(clauses.size() - 1, clauses.get(clauses.size() - 1) + " " + token);
            } else {
                clauses.add(token);
            }
        }

        //DOTCMS - 4127
        List<Field> dateFields = new ArrayList<Field>();
        String tempStructureVarName;
        Structure tempStructure;

        for (String clause: clauses) {

            // getting structure names from query
            if(clause.indexOf('.') >= 0 && (clause.indexOf('.') < clause.indexOf(':'))){

                tempStructureVarName = clause.substring(0, clause.indexOf('.'));
                tempStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(tempStructureVarName);

                if (UtilMethods.isSet(tempStructure) && UtilMethods.isSet(tempStructure.getVelocityVarName())) {
                    List<Field> tempStructureFields = new ArrayList<>(FieldsCache
                            .getFieldsByStructureVariableName(
                                    tempStructure.getVelocityVarName()));

                    for (int pos = 0; pos < tempStructureFields.size(); ) {

                        if (tempStructureFields.get(pos).getFieldType()
                                .equals(Field.FieldType.DATE_TIME.toString()) ||
                                tempStructureFields.get(pos).getFieldType()
                                        .equals(Field.FieldType.DATE.toString()) ||
                                tempStructureFields.get(pos).getFieldType()
                                        .equals(Field.FieldType.TIME.toString())) {
                            ++pos;
                        } else {
                            tempStructureFields.remove(pos);
                        }

                    }

                    dateFields.addAll(tempStructureFields);
                }
            }
        }

        String replace;
        List<RegExMatch> matches;
        String structureVarName;
        for (String clause: clauses) {
            for (Field field: dateFields) {

                structureVarName = CacheLocator.getContentTypeCache()
                        .getStructureByInode(field.getStructureInode()).getVelocityVarName()
                        .toLowerCase();

                if (clause.startsWith(structureVarName + "." + field.getVelocityVarName().toLowerCase() + ":") || clause.startsWith("moddate:")) {
                    replace = new String(clause);
                    if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()) || clause.startsWith("moddate:")) {
                        matches = RegEX.find(replace, "\\[(\\d{1,2}/\\d{1,2}/\\d{4}) TO ");
                        for (RegExMatch regExMatch : matches) {
                            replace = replace.replace("[" + regExMatch.getGroups().get(0).getMatch() + " TO ", "["
                                    + regExMatch.getGroups().get(0).getMatch() + " 00:00:00 TO ");
                        }

                        matches = RegEX.find(replace, " TO (\\d{1,2}/\\d{1,2}/\\d{4})\\]");
                        for (RegExMatch regExMatch : matches) {
                            replace = replace.replace(" TO " + regExMatch.getGroups().get(0).getMatch() + "]", " TO "
                                    + regExMatch.getGroups().get(0).getMatch() + " 23:59:59]");
                        }
                    }

                    //if structureVarName or field.getVelocityVarName() has numbers, we need to split the clause to format only dates
                    if (clause.startsWith(
                            structureVarName + "." + field.getVelocityVarName().toLowerCase()
                                    + ":")) {
                        replace = structureVarName + "." + field.getVelocityVarName()
                                .toLowerCase() + ":" + replaceDateTimeFormatInClause(
                                replace.substring(replace.indexOf(":") + 1));
                    } else {
                        replace = replaceDateTimeFormatInClause(replace);
                    }

                    query = query.replace(clause, replace);

                    break;
                }
            }
        }

        matches = RegEX.find(query, "\\[([0-9]*)(\\*+) TO ");
        for (RegExMatch regExMatch : matches) {
            query = query.replace("[" + regExMatch.getGroups().get(0).getMatch() + regExMatch.getGroups().get(1).getMatch() + " TO ", "["
                    + regExMatch.getGroups().get(0).getMatch() + " TO ");
        }

        matches = RegEX.find(query, " TO ([0-9]*)(\\*+)\\]");
        for (RegExMatch regExMatch : matches) {
            query = query.replace(" TO " + regExMatch.getGroups().get(0).getMatch() + regExMatch.getGroups().get(1).getMatch() + "]", " TO "
                    + regExMatch.getGroups().get(0).getMatch() + "]");
        }

        matches = RegEX.find(query, "\\[([0-9]*) (TO) ([0-9]*)\\]");
        if(matches.isEmpty()){
            matches = RegEX.find(query, "\\[([a-z0-9]*) (TO) ([a-z0-9]*)\\]");
        }
        for (RegExMatch regExMatch : matches) {
            query = query.replace("[" + regExMatch.getGroups().get(0).getMatch() + " TO "
                    + regExMatch.getGroups().get(2).getMatch() + "]", "["
                    + replaceDateTimeFormatInClause(regExMatch.getGroups().get(0).getMatch())
                    + " TO " + replaceDateTimeFormatInClause(
                    regExMatch.getGroups().get(2).getMatch())
                    + "]");
        }

        //https://github.com/elasticsearch/elasticsearch/issues/2980
        if (query.contains( "/" )) {
            query = query.replaceAll( "/", "\\\\/" );
        }

        return query;
    }


    /**
     * Applies supported lucene format to dates in a query
     * @param unformattedDate - Date or range of dates to be formatted
     * @return
     */
    static String replaceDateTimeFormatInClause(final String unformattedDate) {
        String result;

        for (Map.Entry<String, String> pattern: LUCENE_DATE_TIME_FORMAT_PATTERN.entrySet()){
            if (LUCENE_TIME_FORMAT_PATTERNS.contains(pattern.getKey())){
                result = DateUtil
                        .replaceTimeWithFormat(unformattedDate, pattern.getValue(), pattern.getKey());
            } else{
                result = DateUtil
                        .replaceDateTimeWithFormat(unformattedDate, pattern.getValue(), pattern.getKey());
            }

            if (!result.equals(unformattedDate)){
                return result;
            }

        }
        return unformattedDate;
    }

}
