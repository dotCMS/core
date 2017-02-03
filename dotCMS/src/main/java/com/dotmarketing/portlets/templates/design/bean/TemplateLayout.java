package com.dotmarketing.portlets.templates.design.bean;

import java.util.List;

import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.*;

/**
 * Class that represents the javascript parameter for edit the drawed template.
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica
 * @date Apr 20, 2012
 */
public class TemplateLayout {

    public String pageWidth;
    public String width;
    public String layout;

    public String title;

    public boolean header;
    public boolean footer;

    public TemplateLayoutColumn body;
    public TemplateLayoutColumn sidebar;

    public String getPageWidth () {
        return pageWidth;
    }

    public void setPageWidth ( String pageWidth ) {
        this.pageWidth = pageWidth;

        if ( pageWidth.equals( LAYOUT_WIDTH_CLASS_100_PERCENT ) ) {//100%
            this.setWidth( "100%" );
        } else if ( pageWidth.equals( LAYOUT_WIDTH_CLASS_RESPONSIVE ) ) {//Responsive
            this.setWidth( "responsive" );
        } else if ( pageWidth.equals( LAYOUT_WIDTH_CLASS_950 ) ) {//950px
            this.setWidth( "950px" );
        } else if ( pageWidth.equals( LAYOUT_WIDTH_CLASS_975 ) ) {//975px
            this.setWidth( "975px" );
        } else if ( pageWidth.equals( LAYOUT_WIDTH_CLASS_750 ) ) {//750px
            this.setWidth( "750px" );
        }
    }

    public String getLayout () {
        return layout;
    }

    public void setLayout ( String layout ) {
        this.layout = layout;
    }

    public String getWidth () {
        return width;
    }

    public void setWidth ( String width ) {
        this.width = width;
    }

    public String getTitle () {
        return title;
    }

    /**
     * The title of the current template parsed in order to be use as a class and be able to override
     * a template style with the client custom css's
     * <p/>
     * It will remove any non alpha-numeric character, the spaces will be replace them by "-" and will be in lower case
     *
     * @param title
     */
    public void setTitle ( String title ) {
        if ( title == null ) {
            this.title = "";
        } else {
            String escaped = title.replaceAll( "[^A-Za-z0-9 ]", "" );
            escaped = escaped.replaceAll( " ", "-" );
            this.title = escaped.toLowerCase();
        }
    }

    public boolean isHeader () {
        return header;
    }

    public void setHeader ( boolean header ) {
        this.header = header;
    }

    public boolean isFooter () {
        return footer;
    }

    public void setFooter ( boolean footer ) {
        this.footer = footer;
    }

    public TemplateLayoutColumn getBody () {
        return body;
    }

    public void setBody ( List<TemplateLayoutRow> bodyRows ) {

        this.body = new TemplateLayoutColumn();
        this.body.setType( TemplateLayoutColumn.TYPE_BODY );
        this.body.setRows( bodyRows );
    }

    public TemplateLayoutColumn getSidebar () {
        return sidebar;
    }

    public void setSidebar ( List<String> containers, Boolean isPreview ) {

        this.sidebar = new TemplateLayoutColumn();
        this.sidebar.setType( TemplateLayoutColumn.TYPE_SIDEBAR );
        this.sidebar.setContainers( containers );
        this.sidebar.setPreview( isPreview );

        if ( layout.equals( YUI_LAYOUT_LEFT_CLASS_T1 ) ) {//layout-160-left
            this.sidebar.setLocation( TemplateLayoutColumn.LOCATION_LEFT );
            this.sidebar.setWidthPercent( 20 );
        } else if ( layout.equals( YUI_LAYOUT_LEFT_CLASS_T2 ) ) {//layout-240-left
            this.sidebar.setLocation( TemplateLayoutColumn.LOCATION_LEFT );
            this.sidebar.setWidthPercent( 30 );
        } else if ( layout.equals( YUI_LAYOUT_LEFT_CLASS_T3 ) ) {//layout-300-left
            this.sidebar.setLocation( TemplateLayoutColumn.LOCATION_LEFT );
            this.sidebar.setWidthPercent( 40 );
        } else if ( layout.equals( YUI_LAYOUT_RIGHT_CLASS_T4 ) ) {//layout-160-right
            this.sidebar.setLocation( TemplateLayoutColumn.LOCATION_RIGHT );
            this.sidebar.setWidthPercent( 20 );
        } else if ( layout.equals( YUI_LAYOUT_RIGHT_CLASS_T5 ) ) {//layout-240-right
            this.sidebar.setLocation( TemplateLayoutColumn.LOCATION_RIGHT );
            this.sidebar.setWidthPercent( 30 );
        } else if ( layout.equals( YUI_LAYOUT_RIGHT_CLASS_T6 ) ) {//layout-300-right*/
            this.sidebar.setLocation( TemplateLayoutColumn.LOCATION_RIGHT );
            this.sidebar.setWidthPercent( 40 );
        }

    }

}