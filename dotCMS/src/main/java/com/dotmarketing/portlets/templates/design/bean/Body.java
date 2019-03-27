package com.dotmarketing.portlets.templates.design.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.List;

/**
 * It's a {@link com.dotmarketing.portlets.templates.model.Template}'s Body
 */
public class Body implements Serializable{
    private List<TemplateLayoutRow> rows;


    @JsonCreator
    public Body(@JsonProperty("rows") List<TemplateLayoutRow> rows) {
        this.rows = rows;
    }

    public List<TemplateLayoutRow> getRows() {
        return rows;
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
