package com.dotmarketing.util;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class MenuItem implements Serializable{

	private static final long serialVersionUID = -3974910451414246846L;
	
		private String name;
        private String icon;
        private String assoc_portlet;
        private boolean isRendering;
        private boolean isDisplayingReports;
        private boolean isDisplayingContentlets; 
        private boolean isCreatingReport;
        private boolean isDisplayingWebForms;
        private boolean isCreatingCampaign;
        private String position;
        private List<MenuItem> subMenu;
        private Map params;
    
        public MenuItem(
                String name, 
                String icon, 
                String assoc_portlet, 
                boolean isRendering, 
                boolean isDisplayingReports, 
                boolean isDisplayingContentlets,
                boolean isCreatingReport,
                boolean isDisplayingWebForms,
                boolean isCreatingCampaign,
                String position,
                Map params) 
        {
            this.name = name;
            this.icon = icon;
            this.assoc_portlet = assoc_portlet;
            this.isRendering = isRendering;
            this.isDisplayingReports = isDisplayingReports;
            this.isDisplayingContentlets = isDisplayingContentlets;
            this.isCreatingReport = isCreatingReport;
            this.isDisplayingWebForms = isDisplayingWebForms;
            this.isCreatingCampaign = isCreatingCampaign;
            this.position = position;
            this.params = params;
        }
    
        public String getName() {
            return name;
        }
        public String getIcon() {
            return icon;
        }
        public String getAssoc_portlet() {
            return assoc_portlet;
        }
        public boolean isRendering() {
            return isRendering;
        }
        public boolean isDisplayingReports() {
            return isDisplayingReports;
        }
        public boolean isDisplayingContentlets() {
            return isDisplayingContentlets;
        }
        public boolean isCreatingReport() {
            return isCreatingReport;
        }
        public boolean isDisplayingWebForms() {
            return isDisplayingWebForms;
        }
        public boolean isCreatingCampaign() {
            return isCreatingCampaign;
        }
        public String getPosition() {
            return position == null ? "" : position;
        }
        public void setParams(Map params) {
            this.params = params;
        }
        public Map getParams() {
            return params;
        }
        public List<MenuItem> getSubMenu() {
            return subMenu;
        }
        public void setSubMenu(List<MenuItem> items) {
            subMenu = items;
        }
        
        public String toString() {
            return org.apache.commons.lang.builder.ToStringBuilder.reflectionToString(this);
        }
        
        public boolean equals(Object o) {
            if( o == null || !(o instanceof MenuItem) )
                return false;
            return name.equals(((MenuItem)o).getName());
        }
        
        public int hashCode() {
            return name.hashCode();
        }

}

