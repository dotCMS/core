dojo.provide("dotcms.dojo.data.CategoryReadStore");
dojo.require("dojox.data.QueryReadStore");

/*
 Read store for the category grid of the legacy content editor's category field dialog.
 It reads from the categories REST API: the top-level categories when no inode is given,
 otherwise the direct children of that inode, filtered by name, key or variable. The grid's
 paging and sorting are sent as page/per_page/orderby/direction, and each category is handed
 to the grid with the field names its layout reads (category_name, category_key, ...).
 */
dojo.declare("dotcms.dojo.data.CategoryReadStore", dojox.data.QueryReadStore, {

    inode: null,
    filter: "",

    constructor: function (options) {
        this.inode = options && options.inode ? options.inode : null;
        this.filter = options && options.filter ? options.filter : "";
        this.url = this.inode ? "/api/v1/categories/children" : "/api/v1/categories";
    },

    fetch: function (request) {
        var start = request.start || 0;
        var perPage = request.count > 0 && isFinite(request.count) ? request.count : 0;
        var serverQuery = {
            filter: this.filter,
            // with no column sort, the same order the editor always showed: sort order first
            orderby: "sort_order",
            direction: "ASC"
        };

        if (perPage > 0) {
            serverQuery.per_page = perPage;
            serverQuery.page = Math.floor(start / perPage) + 1;
        }

        if (request.sort && request.sort.length > 0 && request.sort[0].attribute) {
            serverQuery.orderby = request.sort[0].attribute;
            serverQuery.direction = request.sort[0].descending ? "DESC" : "ASC";
        }

        if (this.inode) {
            serverQuery.inode = this.inode;
        }

        request.serverQuery = serverQuery;
        return this.inherited(arguments);
    },

    _filterResponse: function (data) {
        var categories = data && data.entity ? data.entity : [];
        var items = dojo.map(categories, function (category) {
            return {
                inode: category.inode,
                category_name: category.categoryName,
                category_key: category.key,
                category_velocity_var_name: category.categoryVelocityVarName,
                sort_order: category.sortOrder,
                keywords: category.keywords
            };
        });
        var numRows = data && data.pagination ? data.pagination.totalEntries : items.length;
        return { items: items, numRows: numRows };
    }
});
