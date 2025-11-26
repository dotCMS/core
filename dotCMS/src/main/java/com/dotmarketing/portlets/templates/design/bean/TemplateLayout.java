package com.dotmarketing.portlets.templates.design.bean;


import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.LAYOUT_WIDTH_CLASS_100_PERCENT;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.LAYOUT_WIDTH_CLASS_750;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.LAYOUT_WIDTH_CLASS_950;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.LAYOUT_WIDTH_CLASS_975;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.LAYOUT_WIDTH_CLASS_RESPONSIVE;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_LAYOUT_LEFT_CLASS_T1;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_LAYOUT_LEFT_CLASS_T2;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_LAYOUT_LEFT_CLASS_T3;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_LAYOUT_RIGHT_CLASS_T4;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_LAYOUT_RIGHT_CLASS_T5;
import static com.dotmarketing.portlets.templates.design.util.DesignTemplateHtmlCssConstants.YUI_LAYOUT_RIGHT_CLASS_T6;

import java.io.Serializable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;

/**
 * Class that represents the javascript parameter for edit the drawed template.
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica
 * @date Apr 20, 2012
 */
public class TemplateLayout implements Serializable {

    @JsonIgnore
    private String pageWidth;
    private String width;
    @JsonIgnore
    private String layout;

    private String title;

    private boolean header;
    private boolean footer;

    private Body body;
    private Sidebar sidebar;

    /**
     * This count how many times the TemplateLayout have changed, but it counts just layout movement
     * it means if A Container is moved, remove or added.
     */
    private int version = 1;

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

    public void setHeader ( final boolean header ) {
        this.header = header;
    }

    public boolean isFooter () {
        return footer;
    }

    public void setFooter ( final boolean footer ) {
        this.footer = footer;
    }

    public Body getBody () {
        return body;
    }

    public void setBody (final Body body) {
        this.body = body;
    }

    public void setBodyRows ( final List<TemplateLayoutRow> bodyRows ) {

        this.body = new Body(bodyRows);
    }

    public Sidebar getSidebar () {
        return sidebar;
    }

    public void setSidebar (final Sidebar sidebar) {
        this.sidebar = sidebar;
    }

    public void setContainers ( final List<ContainerUUID> containers, final Boolean isPreview ) {
        String location = null;
        int widthPercent  = 0;

        if ( layout.equals( YUI_LAYOUT_LEFT_CLASS_T1 ) ) {//layout-160-left
            location = Sidebar.LOCATION_LEFT;
            widthPercent = 20;
        } else if ( layout.equals( YUI_LAYOUT_LEFT_CLASS_T2 ) ) {//layout-240-left
            location = Sidebar.LOCATION_LEFT;
            widthPercent = 30;
        } else if ( layout.equals( YUI_LAYOUT_LEFT_CLASS_T3 ) ) {//layout-300-left
            location =  Sidebar.LOCATION_LEFT;
            widthPercent = 40;
        } else if ( layout.equals( YUI_LAYOUT_RIGHT_CLASS_T4 ) ) {//layout-160-right
            location =  Sidebar.LOCATION_RIGHT;
            widthPercent = 20;
        } else if ( layout.equals( YUI_LAYOUT_RIGHT_CLASS_T5 ) ) {//layout-240-right
            location =  Sidebar.LOCATION_RIGHT;
            widthPercent = 30;
        } else if ( layout.equals( YUI_LAYOUT_RIGHT_CLASS_T6 ) ) {//layout-300-right*/
            location =  Sidebar.LOCATION_RIGHT;
            widthPercent = 40;
        }

        this.sidebar = new Sidebar(containers, location, null, widthPercent);
        this.sidebar.setPreview( isPreview );

    }

    @Override
    public String toString() {
       try {
           return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateLayout that = (TemplateLayout) o;
        return toString().equals(that.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }

    /**
     * Return true if the container exists into the TemplateLayout
     *
     * @param container container
     * @param uuid Container uuid into the TemplateLayout
     * @return
     */
    public boolean existsContainer(final Container container, final String uuid){
        final FileAssetContainerUtil instance = FileAssetContainerUtil.getInstance();
        if (instance.isFileAssetContainer(container)) {
            final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;
            final String containerPath = fileAssetContainer.getPath();

            return this.getContainersUUID().stream()
                    .anyMatch((ContainerUUID containerUUID) -> {
                        final String relativePath = instance.isFullPath(containerUUID.getIdentifier()) ?
                                instance.getRelativePath(containerUUID.getIdentifier()) :
                                containerUUID.getIdentifier();

                        return relativePath.equals(containerPath)
                                    && isTheSameUUID(containerUUID.getUUID(), uuid);
                    }
                    );
        } else {
            return this.getContainersUUID().stream()
                    .anyMatch(containerUUID ->
                            containerUUID.getIdentifier().equals(container.getIdentifier())
                                    && isTheSameUUID(containerUUID.getUUID(), uuid)
                    );
        }


    }

    @JsonIgnore
    public Set<String> getContainersIdentifierOrPath() {

        return this.getContainersUUID()
                .stream()
                .map(ContainerUUID::getIdentifier)
                .collect(Collectors.toSet());
    }

    @JsonIgnore
    public Set<ContainerUUID> getContainersUUID() {

        final Set<ContainerUUID> bodyContainers = body.getRows().stream()
                .flatMap(row -> row.getColumns().stream())
                .flatMap(column -> column.getContainers().stream())
                .collect(Collectors.toSet());

        if (null != this.getSidebar()) {
            final List<ContainerUUID> sideBarContainers = this.getSidebar().getContainers();
            return ImmutableSet.<ContainerUUID> builder()
                    .addAll(sideBarContainers)
                    .addAll(bodyContainers)
                    .build();
        } else {
            return ImmutableSet.<ContainerUUID> builder()
                    .addAll(bodyContainers)
                    .build();
        }
    }

    private boolean isTheSameUUID(final String uuid1, final String uuid2) {
        return ContainerUUID.UUID_LEGACY_VALUE.equals(uuid1) || ContainerUUID.UUID_START_VALUE.equals(uuid1)
            ? ContainerUUID.UUID_LEGACY_VALUE.equals(uuid2) || ContainerUUID.UUID_START_VALUE.equals(uuid2)
            : uuid1.equals(uuid2);
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}