package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsCategory;
import com.dotcms.rendering.velocity.viewtools.CategoriesWebAPI;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.portlets.categories.model.Category;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps the {@link com.dotcms.rendering.velocity.viewtools.CategoriesWebAPI} (categories) into the JS context.
 * @author jsanca
 */
public class CategoriesJsViewTool implements JsViewTool, JsViewContextAware {

    private final CategoriesWebAPI categoriesWebAPI = new CategoriesWebAPI();

    @Override
    public void setViewContext(final ViewContext viewContext) {

        categoriesWebAPI.init(viewContext);
    }

    @Override
    public String getName() {
        return "categories";
    }

    @HostAccess.Export
    public List<Category> getChildrenCategoriesByKey(final String key) {
        return this.categoriesWebAPI.getChildrenCategoriesByKey(key);
    }

    @HostAccess.Export
    public Category getCategoryByKey(final String key) {
        return this.categoriesWebAPI.getCategoryByKey(key);
    }

    @HostAccess.Export
    public List<JsCategory> getChildrenCategories(final JsCategory jsCategory) {
        return this.categoriesWebAPI.getChildrenCategories(jsCategory.getCategoryObject())
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    // proxy the inode
    public List<JsCategory> getChildrenCategories(final Inode inode) {
        return this.categoriesWebAPI.getChildrenCategories(inode)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<JsCategory> getChildrenCategories(final String inode) {
        return this.categoriesWebAPI.getChildrenCategories(inode)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    /**
     * Retrieves the list of categories, their children categories and grand-children categories upto the specified maxDepth.
     *
     * @param inode CategoryInode for which to get the children categories.
     * @param includeGrandChildren
     * @param maxDepth
     * @return
     */
    public List<JsCategory> getChildrenCategories(final String inode,
                                                final boolean includeGrandChildren,
                                                final int maxDepth) {
        return this.categoriesWebAPI.getChildrenCategories(inode, includeGrandChildren, maxDepth)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }


    @HostAccess.Export
    public List<JsCategory> getActiveChildrenCategories(final JsCategory category) {
        return this.categoriesWebAPI.getActiveChildrenCategories(category.getCategoryObject())
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<JsCategory> getActiveChildrenCategoriesByKey(final String key) {
        return this.categoriesWebAPI.getActiveChildrenCategoriesByKey(key)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public List<JsCategory> getActiveChildrenCategories(final Inode inode) {
        return this.categoriesWebAPI.getActiveChildrenCategories(inode)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    public List<JsCategory> getActiveChildrenCategories(final String inode) {
        return this.categoriesWebAPI.getActiveChildrenCategories(inode)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    public List<JsCategory> getActiveChildrenCategoriesOrderByName(final JsCategory cat) {
        return this.categoriesWebAPI.getActiveChildrenCategoriesOrderByName(cat.getCategoryObject())
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    public List<JsCategory> getActiveChildrenCategoriesOrderByName(final Inode inode) {
        return this.categoriesWebAPI.getActiveChildrenCategoriesOrderByName(inode)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    public List<JsCategory> getActiveChildrenCategoriesOrderByName(final String inode) {
        return this.categoriesWebAPI.getActiveChildrenCategoriesOrderByName(inode)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    public List<JsCategory> getActiveChildrenCategoriesByParent(final ArrayList<String> keys) {
       return this.categoriesWebAPI.getActiveChildrenCategoriesByParent(keys)
               .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    /**
     * Retrieves a plain list of all the children categories (any depth) of the
     * given parent category key The list returned is a list of maps, each map
     * has the category and the level of this category belongs
     *
     * E.G. level: 1 cat: Best Practices level: 1 cat: Conferences &
     * Presentations level: 2 cat: second level level: 1 cat: Marketing
     *
     * @param key
     *            parent category key
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ProxyArray getAllActiveChildrenCategoriesByKey(final String key) {

        return ProxyArray.fromList(this.categoriesWebAPI.getAllActiveChildrenCategoriesByKey(key).stream().map(categoryMap -> {
            final Map map = categoryMap;
            return ProxyHashMap.from(map);
        }).collect(Collectors.toList()));
    }

    @HostAccess.Export
    /**
     * Retrieves a plain list of all the children categories (any depth) of the
     * given parent inode The list returned is a list of maps, each map has the
     * category and the level of this category belongs
     *
     * E.G. level: 1 cat: Best Practices level: 1 cat: Conferences &
     * Presentations level: 2 cat: second level level: 1 cat: Marketing
     *
     * @param inode
     *            parent inode
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ProxyArray getAllActiveChildrenCategories(final Inode inode) {
       return ProxyArray.fromList(this.categoriesWebAPI.getAllActiveChildrenCategories(inode).stream().map(categoryMap -> {
            final Map map = categoryMap;
            return ProxyHashMap.from(map);
        }).collect(Collectors.toList()));
    }

    @HostAccess.Export
    /**
     * Retrieves a plain list of all the children categories (any depth) of the
     * given parent inode The list returned is a list of maps, each map has the
     * category and the level of this category belongs
     *
     * E.G. level: 1 cat: Best Practices level: 1 cat: Conferences &
     * Presentations level: 2 cat: second level level: 1 cat: Marketing
     *
     * @param inode
     *            parent inode
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ProxyArray getAllActiveChildrenCategories(final String inode) {

        return ProxyArray.fromList(this.categoriesWebAPI.getAllActiveChildrenCategories(inode).stream().map(categoryMap -> {
            final Map map = categoryMap;
            return ProxyHashMap.from(map);
        }).collect(Collectors.toList()));
    }

    @HostAccess.Export
    public List<JsCategory> getInodeCategories(final String inode) {
        return this.categoriesWebAPI.getInodeCategories(inode)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    public List<JsCategory> getInodeCategories(final Inode inodeObj) {
        return this.categoriesWebAPI.getInodeCategories(inodeObj)
                .stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    public JsCategory getCategoryByInode(final String inode) {
        return new JsCategory(this.categoriesWebAPI.getCategoryByInode(inode));
    }

    @HostAccess.Export
    public String getCategoryKeyByContentlet(final String contentletInode) {
        return this.categoriesWebAPI.getCategoryKeyByContentlet(contentletInode);
    }


    @HostAccess.Export
    public List<JsCategory> filterCategoriesByUserPermissions(List<Object> catInodes) {
        return this.categoriesWebAPI.filterCategoriesByUserPermissions(catInodes).stream().map(JsCategory::new).collect(Collectors.toList());
    }

    @HostAccess.Export
    public List<String> fetchCategoriesInodes(final List<JsCategory> cats) {
        return this.categoriesWebAPI.fetchCategoriesInodes(cats.stream().map(JsCategory::getCategoryObject).collect(Collectors.toList()));
    }

    @HostAccess.Export
    public List<String> fetchCategoriesNames(final List<JsCategory> cats) {
        return this.categoriesWebAPI.fetchCategoriesNames(cats.stream().map(JsCategory::getCategoryObject).collect(Collectors.toList()));
    }

    @HostAccess.Export
    public List<String> fetchCategoriesKeys(final List<JsCategory> cats) {
        return this.categoriesWebAPI.fetchCategoriesKeys(cats.stream().map(JsCategory::getCategoryObject).collect(Collectors.toList()));
    }
}
