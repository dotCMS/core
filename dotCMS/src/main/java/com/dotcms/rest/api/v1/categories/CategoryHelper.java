package com.dotcms.rest.api.v1.categories;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Helper for categories
 *
 * @author Hassan Mustafa Baig
 */
public class CategoryHelper {

    private final CategoryAPI categoryAPI;

    public CategoryHelper() {
        this(APILocator.getCategoryAPI());
    }

    @VisibleForTesting
    public CategoryHelper(final CategoryAPI categoryAPI) {

        this.categoryAPI = categoryAPI;
    }

    public Host getHost(final String hostId, final Supplier<Host> hostSupplier) {

        if (UtilMethods.isSet(hostId)) {

            return Try.of(
                            () -> APILocator.getHostAPI().find(hostId, APILocator.systemUser(), false))
                    .getOrElse(hostSupplier);
        }

        return hostSupplier.get();
    }

    public CategoryView toCategoryView(final Category category, final User user) {

        return new CategoryView.Builder()
                .inode(category.getInode())
                .description(category.getDescription())
                .keywords(category.getKeywords())
                .key(category.getKey())
                .categoryName(category.getCategoryName())
                .active(category.isActive())
                .sortOrder(category.getSortOrder())
                .categoryVelocityVarName(category.getCategoryVelocityVarName())
                .build();
    }

    public void AddOrUpdateCategory(final User user, final String contextInode,final BufferedReader br,final Boolean merge) throws IOException, Exception {
        CsvReader csvreader = new CsvReader(br);
        csvreader.setSafetySwitch(false);
        csvreader.readHeaders();
        String[] csvLine;

        while (csvreader.readRecord()) {
            csvLine = csvreader.getValues();
            try {
                AddOrUpdateCategory(user, true, contextInode, csvLine[0], csvLine[2], csvLine[1], null, csvLine[3], merge);

            } catch(Exception e) {
                Logger.error(this, "Error trying to save/update the categories csv row: name=" +csvLine[0]+ ", variable=" + csvLine[2] + ", key=" + csvLine[1] + ", sort=" + csvLine[3] , e);
            }
        }

        csvreader.close();
        br.close();
    }

    private Integer AddOrUpdateCategory(final User user, final Boolean isSave, final String inode, final String name, final String var, final String key, final String keywords, final String sort, final boolean isMerge)
            throws Exception {

        Category parent = null;
        Category cat = new Category();
        cat.setCategoryName(name);
        cat.setKey(key);
        cat.setCategoryVelocityVarName(var);
        cat.setSortOrder(sort);
        cat.setKeywords(keywords);

        if(UtilMethods.isSet(inode)){
            if(!isSave){//edit
                cat.setInode(inode);
                final Category finalCat = cat;//this is to be able to use the try.of
                parent = Try.of(()->categoryAPI.getParents(finalCat,user,false).get(0)).getOrNull();
            }else{//save
                parent = categoryAPI.find(inode, user, false);
            }
        }

        setVelocityVarName(cat, var, name);

        if(isMerge) { // add/edit

            if(isSave) { // Importing
                if(UtilMethods.isSet(key)) {
                    cat = categoryAPI.findByKey(key, user, false);
                    if(cat==null) {
                        cat = new Category();
                        cat.setKey(key);
                    }

                    cat.setCategoryName(name);
                    setVelocityVarName(cat, var, name);
                    cat.setSortOrder(sort);
                }
            } else { // Editing
                cat = categoryAPI.find(inode, user, false);
                cat.setCategoryName(name);
                setVelocityVarName(cat, var, name);
                cat.setKeywords(keywords);
                cat.setKey(key);
            }

        } else { // replace
            cat.setCategoryName(name);
            setVelocityVarName(cat, var, name);
            cat.setSortOrder(sort);
            cat.setKey(key);
        }

        try {
            categoryAPI.save(parent, cat, user, false);
        } catch (DotSecurityException e) {
            return 1;
        }

        return 0;
    }

    private void setVelocityVarName(Category cat, String catvelvar, String catName) throws DotDataException, DotSecurityException {
        Boolean Proceed=false;
        if(!UtilMethods.isSet(catvelvar)){
            catvelvar= StringUtils.camelCaseLower(catName);
            Proceed=true;
        }
        if(!InodeUtils.isSet(cat.getInode())|| Proceed){
            if(VelocityUtil.isNotAllowedVelocityVariableName(catvelvar)){
                catvelvar= catvelvar + "Field";
            }
            catvelvar = categoryAPI.suggestVelocityVarName(catvelvar);
            cat.setCategoryVelocityVarName(catvelvar);
        }
    }
}
