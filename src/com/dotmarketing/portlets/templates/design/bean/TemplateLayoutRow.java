package com.dotmarketing.portlets.templates.design.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean that represent a single HTML select/option when we try to edit a drawed template.
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica
 * @date Apr 23, 2012
 */
public class TemplateLayoutRow {

    private int identifier;
    private String value;
    private String id;

    public int columnsCount;
    public Integer[] gridWidths;
    public List<TemplateLayoutColumn> columns;

    private String YUI_ONE_COLUMN_CLASS = "1";//1 Column (100)
    private String YUI_TWO_COLUMN_CLASS_GC = "yui-gc-template";//2 Column (66/33)
    private String YUI_TWO_COLUMN_CLASS_GD = "yui-gd-template";//2 Column (33/66)
    private String YUI_TWO_COLUMN_CLASS_GE = "yui-ge-template";//2 Column (75/25)
    private String YUI_TWO_COLUMN_CLASS_GF = "yui-gf-template";//2 Column (25/75)
    private String YUI_THREE_COLUMN_CLASS = "yui-gb-template";//3 Column (33/33/33)

    public int getIdentifier () {
        return identifier;
    }

    public void setIdentifier ( int identifier ) {
        this.identifier = identifier;
    }

    public String getValue () {
        return value;
    }

    /**
     * Sets the current value for this row (YIU layout class). Based on this
     * class we will set the number of columns and the widths for every column
     * on this row
     *
     * @param value
     */
    public void setValue ( String value ) {
        this.value = value;

        //Now based on this value (YUI layout class) we will set the row layout type
        if ( value.equals( YUI_ONE_COLUMN_CLASS ) ) {//1 Column (100)
            columnsCount = 1;
            gridWidths = new Integer[]{100};
        } else if ( value.equals( YUI_TWO_COLUMN_CLASS_GC ) ) {//2 Column (66/33)
            columnsCount = 2;
            gridWidths = new Integer[]{66, 33};
        } else if ( value.equals( YUI_TWO_COLUMN_CLASS_GD ) ) {//2 Column (33/66)
            columnsCount = 2;
            gridWidths = new Integer[]{33, 66};
        } else if ( value.equals( YUI_TWO_COLUMN_CLASS_GE ) ) {//2 Column (75/25)
            columnsCount = 2;
            gridWidths = new Integer[]{75, 25};
        } else if ( value.equals( YUI_TWO_COLUMN_CLASS_GF ) ) {//2 Column (25/75)
            columnsCount = 2;
            gridWidths = new Integer[]{25, 75};
        } else if ( value.equals( YUI_THREE_COLUMN_CLASS ) ) {//3 Column (33/33/33)
            columnsCount = 3;
            gridWidths = new Integer[]{33, 33, 33};
        }
    }

    public String getId () {
        return id;
    }

    public void setId ( String id ) {
        this.id = id;
    }

    public List<TemplateLayoutColumn> getColumns () {
        return columns;
    }

    /**
     * Adds a container to this row. Adding a container means to add a new column on the row.
     *
     * @param container
     */
    public void addContainer ( String container ) {

        if ( columns == null ) {
            columns = new ArrayList<TemplateLayoutColumn>();
        }

        //Creating a new column for this row
        TemplateLayoutColumn column = new TemplateLayoutColumn();
        column.setContainer( container );

        //Now calculate the width percent for this column
        column.setWidthPercent( gridWidths[columns.size()] );

        //Finally add this column to this row...
        columns.add( column );
    }

}