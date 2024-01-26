package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class LayoutDataGen extends AbstractDataGen<Layout> {

    private final long currentTime = System.currentTimeMillis();
    private String name = " Layout " + currentTime;
    private String description = " Description " + currentTime; ;
    private int tabOrder = new Random().nextInt();
    private List<String> portletIds = new ArrayList<>();

    public LayoutDataGen name(final String name) {
        this.name = name;
        return this;
    }

    public LayoutDataGen description(final String description) {
        this.description = description;
        return this;
    }

    public LayoutDataGen tabOrder(final int tabOrder) {
        this.tabOrder = tabOrder;
        return this;
    }

    public LayoutDataGen portletIds(final String... ids) {
        this.portletIds.addAll(Arrays.asList(ids));
        return this;
    }

    @Override
    public Layout next() {
        final Layout layout = new Layout();
        layout.setDescription(description);
        layout.setName(name);
        layout.setPortletIds(new ArrayList<>(portletIds));
        layout.setTabOrder(tabOrder);
        return layout;
    }

    @WrapInTransaction
    @Override
    public Layout persist(Layout object) {
        try {
            final Layout layout = next();
            APILocator.getLayoutAPI().saveLayout(layout);
            if(UtilMethods.isSet(portletIds)) {
                APILocator.getLayoutAPI().setPortletIdsToLayout(layout, portletIds);
            }
            return layout;
        } catch (DotDataException e) {
            throw new RuntimeException("Unable to persist layout.", e);
        }
    }
}
