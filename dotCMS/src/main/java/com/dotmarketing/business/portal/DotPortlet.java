package com.dotmarketing.business.portal;
import com.dotmarketing.business.portal.DotPortletBuilder;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.liferay.portal.model.Portlet;
import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;

import java.util.Map;

@Value.Immutable
@Value.Style(
        overshadowImplementation = true,
        typeBuilder = "*Builder",
        depluralize = true,
        visibility = ImplementationVisibility.PACKAGE,
        builderVisibility = BuilderVisibility.PACKAGE,
        implementationNestedInBuilder = true,
        deepImmutablesDetection = false
)
@JsonSerialize(as = DotPortlet.class)
@JsonDeserialize(builder = DotPortlet.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "portlet")
public interface DotPortlet extends XmlSerializable<DotPortlet> {


    @JacksonXmlProperty(localName = "portlet-name")
    @JsonProperty("portlet-name")
    String getPortletId();

    @JacksonXmlProperty(localName = "portlet-class")
    @JsonProperty("portlet-class")
    String getPortletClass();

    @JacksonXmlElementWrapper(useWrapping = false, localName = "init-params")
    @JacksonXmlProperty(localName = "init-param")
    List<InitParam> getInitParams();

    @JsonIgnore
    @Value.Derived
    default Map<String, String> getInitParamsMap() {
        return getInitParams().stream()
                .collect(Collectors.toMap(InitParam::getName, InitParam::getValue));
    }


    static DotPortlet from(Portlet portlet) {
        return DotPortlet.builder()
                .portletId(portlet.getPortletId())
                .portletClass(portlet.getPortletClass())
                .addAllInitParams(portlet.getInitParams().entrySet().stream()
                        .map(e -> InitParam.of(e.getKey(), e.getValue()))
                        .collect(Collectors.toList()))
                .build();


    }

    default Portlet toPortlet() {
        return new Portlet(getPortletId(), getPortletClass(), getInitParamsMap());
    }

    class Builder extends DotPortletBuilder implements XmlImmutableBuilder<DotPortlet> {
    }

    static Builder builder() {
        return new Builder();
    }
}