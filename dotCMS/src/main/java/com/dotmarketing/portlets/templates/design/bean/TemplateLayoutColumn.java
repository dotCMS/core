package com.dotmarketing.portlets.templates.design.bean;

import com.dotmarketing.portlets.templates.design.util.PreviewTemplateUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Jonathan Gamba
 * Date: 10/25/12
 */
public class TemplateLayoutColumn extends ContainerHolder {

    private Integer widthPercent;
    private Integer width;
    private int leftIndex;

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
            switch (this.width) {
                case 12:
                    this.widthPercent = 100;
                    break;
                case 9:
                    this.widthPercent = 75;
                    break;
                case 8:
                    this.widthPercent = 66;
                    break;
                case 6:
                    this.widthPercent = 50;
                    break;
                case 4:
                    this.widthPercent = 33;
                    break;
                case 3:
                    this.widthPercent = 25;
            }
        }

        return widthPercent;
    }

    public void setWidthPercent ( Integer widthPercent ) {
        this.widthPercent = widthPercent;
    }

    public Integer getWidth () {
        return width;
    }

    public int getLeftIndex() {
        return leftIndex;
    }
}