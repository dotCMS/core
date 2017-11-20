package com.dotmarketing.portlets.templates.design.bean;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Created by Jonathan Gamba
 * Date: 10/25/12
 */
public class TemplateLayoutColumn extends ContainerHolder {

    private Integer widthPercent;
    private Integer width;
    private int leftIndex = -1;

    private Map<Integer, Integer> mapWithWidthPercent = map(12, 100, 9, 75, 8, 66, 6,50, 4, 33, 3,25);
    private Map<Integer, Integer> mapWidthPercentWith = map(100, 12, 75, 9, 66, 8, 50,6, 33, 4, 25,3);

    @JsonCreator
    public TemplateLayoutColumn(@JsonProperty("containers") List<String> containers,
                                @JsonProperty("widthPercent") final int widthPercent,
                                @JsonProperty("leftIndex") final int leftIndex) {
        super(containers);

        this.widthPercent = widthPercent;
        this.leftIndex = leftIndex;
    }

    public Integer getWidthPercent () {
        if (widthPercent == null || widthPercent == 0){
            this.widthPercent = this.mapWithWidthPercent.get(this.width);
        }

        return widthPercent;
    }

    public Integer getWidth () {

        if (width == null || width == 0) {
            this.width = this.mapWidthPercentWith.get(this.widthPercent);
        }

        return width;
    }

    public int getLeftIndex() {
        return leftIndex;
    }
}