package com.dotmarketing.business.portal;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value;

import java.util.List;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(
        typeBuilder = "*Builder",
        depluralize = true,
        visibility = ImplementationVisibility.PRIVATE,
        builderVisibility = BuilderVisibility.PUBLIC,
        deepImmutablesDetection = true
)
@JacksonXmlRootElement(localName = "portlet-app")
@JsonSerialize(as = PortletList.class)
@JsonDeserialize(builder = PortletList.Builder.class)
public interface PortletList extends XMLSerializable {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "portlet")
    @JsonProperty("portlet")
    List<DotPortlet> getPortlets();

    class Builder extends PortletListBuilder implements XMLEnabledBuilder<PortletList> {
    }

    static Builder builder() {
        return new Builder();
    }
}