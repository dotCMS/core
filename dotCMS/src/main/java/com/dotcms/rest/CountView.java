package com.dotcms.rest;

/**
 * This class encapsulates a single count
 * @author jsanca
 */
public class CountView {

    private int count;

    public CountView(final int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
