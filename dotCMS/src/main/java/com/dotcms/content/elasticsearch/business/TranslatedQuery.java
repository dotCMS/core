package com.dotcms.content.elasticsearch.business;

import static com.dotmarketing.util.StringUtils.lowercaseStringExceptMatchingTokens;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.NumberUtil;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Represents a translated query with processed query string and sort information. This class
 * provides a search engine agnostic way to represent query translation results.
 */
public class TranslatedQuery implements Serializable {

    public static final String LUCENE_RESERVED_KEYWORDS_REGEX = "OR|AND|NOT|TO";

    private static final long serialVersionUID = 1L;
    private String query;
    private String sortBy;

    public TranslatedQuery() {
    }

    public TranslatedQuery(String query, String sortBy) {
        this.query = query;
        this.sortBy = sortBy;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    @Override
    public String toString() {
        return "TranslatedQuery{query='" + query + "', sortBy='" + sortBy + "'}";
    }

    /**
     * Takes a dotCMS-generated query and translates it into valid terms and
     * keywords for accessing the Elastic index.
     *
     * @param query
     *            - The Lucene query.
     * @param sortBy
     *            - The parameter used to order the results.
     * @return The translated query used to search content in our Elastic index.
     */
    public static TranslatedQuery translateQuery(String query, String sortBy) {

        query = getQueryWithValidContentTypes(query);

        TranslatedQuery result = CacheLocator.getContentletCache()
                .getTranslatedQuery(query + " --- " + sortBy);

        if(result != null) {
            result.setQuery(lowercaseStringExceptMatchingTokens(result.getQuery(),
                    LUCENE_RESERVED_KEYWORDS_REGEX));
            return result;
        }

        result = new TranslatedQuery();

        String originalQuery = query;
        Structure st;
        String stInodestr = "structureInode";
        String stInodeStrLowered = "structureinode";
        String stNameStrLowered = "structurename";
        String contentTypeInodeStr = "contentTypeInode";

        if (query.contains(stNameStrLowered))
            query = query.replace(stNameStrLowered,"structureName");

        if (query.contains(stInodeStrLowered))
            query = query.replace(stInodeStrLowered,stInodestr);

        if (query.toLowerCase().contains(contentTypeInodeStr.toLowerCase()))
            query = query.replace(contentTypeInodeStr,stInodestr);

        if (query.contains(stInodestr)) {
            // get structure information
            int index = query.indexOf(stInodestr) + stInodestr.length() + 1;
            String inode = null;
            try {
                inode = query.substring(index, query.indexOf(" ", index));
            } catch (StringIndexOutOfBoundsException e) {
                Logger.debug(ESContentFactoryImpl.class, e.toString());
                inode = query.substring(index);
            }
            st = CacheLocator.getContentTypeCache().getStructureByInode(inode);
            if (!InodeUtils.isSet(st.getInode()) || !UtilMethods.isSet(st.getVelocityVarName())) {
                Logger.error(ESContentFactoryImpl.class,
                        "Unable to find Structure or Structure Velocity Variable Name from passed in structureInode Query : "
                                + query);

                result.setQuery(query);
                result.setSortBy(sortBy);

                return result;
            }

            // replace structureInode
            query = query.replace("structureInode:"+inode, "structureName:" + st.getVelocityVarName());

            // handle the field translation
            List<Field> fields = FieldsCache.getFieldsByStructureVariableName(st.getVelocityVarName());
            Map<String, Field> fieldsMap;
            try {
                fieldsMap = UtilMethods.convertListToHashMap(fields, "getFieldContentlet", String.class);
            } catch (Exception e) {
                Logger.error(ESContentFactoryImpl.class, e.getMessage(), e);
                result.setQuery(query);
                result.setSortBy(sortBy);
                return result;
            }
            String[] matcher = { "date", "text", "text_area", "integer", "float", "bool" };
            for (String match : matcher) {
                if (query.contains(match)) {
                    List<RegExMatch> mathes = RegEX.find(query, match + "([1-9][1-5]?):");
                    for (RegExMatch regExMatch : mathes) {
                        String oldField = regExMatch.getMatch().substring(0, regExMatch.getMatch().indexOf(":"));
                        query = query.replace(oldField, st.getVelocityVarName() + "."
                                + fieldsMap.get(oldField).getVelocityVarName());
                    }
                }
            }

            // handle categories
            String catRegExpr = "((c(([a-f0-9]{8,8})\\-([a-f0-9]{4,4})\\-([a-f0-9]{4,4})\\-([a-f0-9]{4,4})\\-([a-f0-9]{12,12}))c:on)|(c[0-9]*c:on))";//DOTCMS-4564
            if (RegEX.contains(query, catRegExpr)) {
                List<RegExMatch> mathes = RegEX.find(query, catRegExpr);
                for (RegExMatch regExMatch : mathes) {
                    try {
                        String catInode = regExMatch.getGroups().get(0).getMatch().substring(1, regExMatch.getGroups().get(0).getMatch().indexOf("c:on"));
                        query = query.replace(regExMatch.getMatch(), "categories:"
                                + APILocator.getCategoryAPI().find(catInode,
                                APILocator.getUserAPI().getSystemUser(), true).getCategoryVelocityVarName());
                    } catch (Exception e) {
                        Logger.error(ESContentFactoryImpl.class, e.getMessage() + " : Error loading category", e);
                        result.setQuery(query);
                        result.setSortBy(sortBy);
                        return result;
                    }
                }
            }

            result.setSortBy(translateQuerySortBy(sortBy, originalQuery));
        }



        //Pad Numbers
        final List<RegExMatch> numberMatches = RegEX.find(query, "(\\w+)\\.(\\w+):([0-9]+\\.?[0-9]+ |\\.?[0-9]+ |[0-9]+\\.?[0-9]+$|\\.?[0-9]+$)");
        if(numberMatches != null && numberMatches.size() > 0){
            for (final RegExMatch numberMatch : numberMatches) {
                final List<Field> fields = FieldsCache.getFieldsByStructureVariableName(numberMatch.getGroups().get(0).getMatch());
                for (final Field field : fields) {
                    if(field.getVelocityVarName().equalsIgnoreCase(numberMatch.getGroups().get(1).getMatch())){
                        if (field.getFieldContentlet().startsWith("float")) {
                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + numberMatch.getGroups().get(2).getMatch().trim(),
                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + NumberUtil.pad(Float.parseFloat(numberMatch.getGroups().get(2).getMatch().trim())) + " ");
                        }else if(field.getFieldContentlet().startsWith("integer")) {
                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + numberMatch.getGroups().get(2).getMatch().trim(),
                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + NumberUtil.pad(Long.parseLong(numberMatch.getGroups().get(2).getMatch().trim())) + " ");
                        }else if(field.getFieldContentlet().startsWith("bool")) {
                            final String oldSubQuery = numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + numberMatch.getGroups().get(2).getMatch();
                            final String oldFieldBooleanValue = oldSubQuery.substring(oldSubQuery.indexOf(":")+1,oldSubQuery.indexOf(":") + 2);
                            String newFieldBooleanValue="";
                            if(oldFieldBooleanValue.equals("1") || oldFieldBooleanValue.equals("true"))
                                newFieldBooleanValue = "true";
                            else if(oldFieldBooleanValue.equals("0") || oldFieldBooleanValue.equals("false"))
                                newFieldBooleanValue = "false";
                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + numberMatch.getGroups().get(2).getMatch(),
                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":" + newFieldBooleanValue + " ");
                        }
                    }
                }
            }
        }

        if (UtilMethods.isSet(sortBy)) {
            result.setSortBy(translateQuerySortBy(sortBy, query));
        }

        // DOTCMS-6247
        query = lowercaseStringExceptMatchingTokens(query, LUCENE_RESERVED_KEYWORDS_REGEX);

        //Pad NumericalRange Numbers
        final List<RegExMatch> numberRangeMatches = RegEX.find(query, "(\\w+)\\.(\\w+):\\[(([0-9]+\\.?[0-9]+ |\\.?[0-9]+ |[0-9]+\\.?[0-9]+|\\.?[0-9]+) to ([0-9]+\\.?[0-9]+ |\\.?[0-9]+ |[0-9]+\\.?[0-9]+|\\.?[0-9]+))\\]");
        if(numberRangeMatches != null && numberRangeMatches.size() > 0){
            for (final RegExMatch numberMatch : numberRangeMatches) {
                final List<Field> fields = FieldsCache.getFieldsByStructureVariableName(numberMatch.getGroups().get(0).getMatch());
                for (final Field field : fields) {
                    if(field.getVelocityVarName().equalsIgnoreCase(numberMatch.getGroups().get(1).getMatch())){
                        if (field.getFieldContentlet().startsWith("float")) {
                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":[" + numberMatch.getGroups().get(3).getMatch().trim() + " to " + numberMatch.getGroups().get(4).getMatch().trim() +"]",
                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":[" + NumberUtil.pad(Float.parseFloat(numberMatch.getGroups().get(3).getMatch().trim())) + " TO " + NumberUtil.pad(Float.parseFloat(numberMatch.getGroups().get(4).getMatch().trim())) + "]");
                        }else if(field.getFieldContentlet().startsWith("integer")) {
                            query = query.replace(numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":[" + numberMatch.getGroups().get(3).getMatch().trim() + " to " + numberMatch.getGroups().get(4).getMatch().trim() +"]",
                                    numberMatch.getGroups().get(0).getMatch() + "." + numberMatch.getGroups().get(1).getMatch() + ":[" + NumberUtil.pad(Long.parseLong(numberMatch.getGroups().get(3).getMatch().trim())) + " TO " + NumberUtil.pad(Long.parseLong(numberMatch.getGroups().get(4).getMatch().trim())) + "]");
                        }
                    }
                }
            }
        }


        result.setQuery(query.trim());

        CacheLocator.getContentletCache().addTranslatedQuery(
                originalQuery + " --- " + sortBy, result);

        return result;
    }


    /**
     *
     * @param sortBy
     * @param originalQuery
     * @return
     */
    private static String translateQuerySortBy(String sortBy, String originalQuery) {

        if(sortBy == null)
            return null;

        List<RegExMatch> matches = RegEX.find(originalQuery,  "structureName:([^\\s)]+)");
        List<Field> fields = null;
        Structure structure = null;
        if(matches.size() > 0) {
            String structureName = matches.get(0).getGroups().get(0).getMatch();
            fields = FieldsCache.getFieldsByStructureVariableName(structureName);
            structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(structureName);
        } else {
            matches = RegEX.find(originalQuery, "structureInode:([^\\s)]+)");
            if(matches.size() > 0) {
                String structureInode = matches.get(0).getGroups().get(0).getMatch();
                fields = FieldsCache.getFieldsByStructureInode(structureInode);
                structure = CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
            }
        }

        if(fields == null)
            return sortBy;

        Map<String, Field> fieldsMap;
        try {
            fieldsMap = UtilMethods.convertListToHashMap(fields, "getFieldContentlet", String.class);
        } catch (Exception e) {
            Logger.error(ESContentFactoryImpl.class, e.getMessage(), e);
            return sortBy;
        }

        String[] matcher = { "date", "text", "text_area", "integer", "float", "bool" };
        List<RegExMatch> mathes;
        String oldField, oldFieldTrim, newField;
        for (String match : matcher) {
            if (sortBy.contains(match)) {
                mathes = RegEX.find(sortBy, match + "([1-9][1-5]?)");
                for (RegExMatch regExMatch : mathes) {
                    oldField = regExMatch.getMatch();
                    oldFieldTrim = oldField.replaceAll("[,\\s]", "");
                    if(fieldsMap.get(oldFieldTrim) != null) {
                        newField = oldField.replace(oldFieldTrim, structure.getVelocityVarName() + "." + fieldsMap.get(oldFieldTrim).getVelocityVarName());
                        sortBy = sortBy.replace(oldField, newField);
                    }
                }
            }
        }

        return sortBy;
    }

    /**
     * This method filters out some content types depending on the license level
     * @param query String with the lucene query where the filter will be applied
     * @return String Query excluding non allowed content types
     */
    private static String getQueryWithValidContentTypes(final String query){
        if (!LicenseManager.getInstance().isEnterprise()) {
            final StringBuilder queryBuilder = new StringBuilder(query);
            queryBuilder.append(" -baseType:" + BaseContentType.PERSONA.getType() + " ");
            queryBuilder.append(" -basetype:" + BaseContentType.FORM.getType() + " ");
            return queryBuilder.toString();
        }

        return query;
    }
}
