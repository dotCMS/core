package com.dotmarketing.portlets.templates.design.bean;

import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_FOUR_COLUMN_CLASS;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_ONE_COLUMN_CLASS;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_THREE_COLUMN_CLASS;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_TWO_COLUMN_CLASS_G;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_TWO_COLUMN_CLASS_GC;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_TWO_COLUMN_CLASS_GD;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_TWO_COLUMN_CLASS_GE;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_TWO_COLUMN_CLASS_GF;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean that represent a single HTML select/option when we try to edit a drawed template.
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica
 * @date Apr 23, 2012
 */
public class TemplateLayoutRow implements Serializable {

  private int identifier;
  private String value;
  private String id;

  private Integer[] gridWidths;
  private List<TemplateLayoutColumn> columns;
  private final Map<Integer, TemplateLayoutColumn> columnMap;;

  @JsonCreator
  public TemplateLayoutRow(@JsonProperty("columns") List<TemplateLayoutColumn> columns) {
    this.columns = columns;
    columnMap = new HashMap<>();
    if (null != columns) {
      for (TemplateLayoutColumn column : columns) {
        columnMap.put(column.getLeftOffset(), column);
      }
    }
  }

  public TemplateLayoutRow() {

    this(null);
  }

  public TemplateLayoutColumn getColumn(final String column) {
    return columnMap.get(Integer.valueOf(column));
  }

  public TemplateLayoutColumn getColumn(final int column) {

    return columnMap.get(column);
  }

  public int getIdentifier() {
    return identifier;
  }

  public void setIdentifier(int identifier) {
    this.identifier = identifier;
  }

  public String getValue() {
    return value;
  }

  /**
   * Sets the current value for this row (YIU layout class). Based on this class we will set the
   * number of columns and the widths for every column on this row
   *
   * @param value
   */
  public void setValue(String value) {

    if (value == null) {
      value = "";
    }
    this.value = value;

    // Now based on this value (YUI layout class) we will set the row layout type
    if (value.equals(YUI_ONE_COLUMN_CLASS)) { // 1 Column (100)
      gridWidths = new Integer[] {100};
    } else if (value.equals(YUI_TWO_COLUMN_CLASS_G)) { // 2 Column (50/50)
      gridWidths = new Integer[] {50, 50};
    } else if (value.equals(YUI_TWO_COLUMN_CLASS_GC)) { // 2 Column (66/33)
      gridWidths = new Integer[] {66, 33};
    } else if (value.equals(YUI_TWO_COLUMN_CLASS_GD)) { // 2 Column (33/66)
      gridWidths = new Integer[] {33, 66};
    } else if (value.equals(YUI_TWO_COLUMN_CLASS_GE)) { // 2 Column (75/25)
      gridWidths = new Integer[] {75, 25};
    } else if (value.equals(YUI_TWO_COLUMN_CLASS_GF)) { // 2 Column (25/75)
      gridWidths = new Integer[] {25, 75};
    } else if (value.equals(YUI_THREE_COLUMN_CLASS)) { // 3 Column (33/33/33)
      gridWidths = new Integer[] {33, 33, 33};
    } else if (value.equals(YUI_FOUR_COLUMN_CLASS)) { // 4 Column (25/25/25/25)
      gridWidths = new Integer[] {25, 25, 25, 25};
    } else { // 1 Column (100)
      gridWidths = new Integer[] {100};
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<TemplateLayoutColumn> getColumns() {
    return columns;
  }

  /**
   * Adds containers to this row. Adding a list of containers means to add a new column in the row.
   *
   * @param containers
   * @param isPreview
   */
  public void addColumnContainers(List<ContainerUUID> containers, Boolean isPreview) {

    if (columns == null) {
      columns = new ArrayList<TemplateLayoutColumn>();
    }

    int leftIndex = 1;

    for (TemplateLayoutColumn column : columns) {
      leftIndex += column.getWidth();
    }

    // Creating a new column for this row
    TemplateLayoutColumn column =
        new TemplateLayoutColumn(containers, gridWidths[columns.size()], leftIndex);

    // Is preview mode??
    column.setPreview(isPreview);

    // Finally add this column to this row...
    columns.add(column);
    columnMap.put(leftIndex, column);
  }

  @Override
  public String toString() {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return super.toString();
    }
  }
}
