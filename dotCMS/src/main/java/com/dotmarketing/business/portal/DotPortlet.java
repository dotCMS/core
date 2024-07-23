package com.dotmarketing.business.portal;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.liferay.portal.model.Portlet;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.immutables.value.Value;
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
        deepImmutablesDetection = true
)
@JsonSerialize(as = DotPortlet.class)
@JsonDeserialize(builder = DotPortlet.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = false)
@XmlRootElement(name = "portlet")
@XmlAccessorType(XmlAccessType.FIELD)
public interface DotPortlet extends XMLSerializable {

    @XmlElement(name = "portlet-name")
    @JsonProperty("portlet-name")
    String getPortletId();

    @XmlElement(name = "portlet-class")
    @JsonProperty("portlet-class")
    String getPortletClass();

   // @XmlElementWrapper(name = "init-params")
    @XmlElement(name = "init-param")
    @JsonGetter("init-param")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "init-params")
    default List<InitParam> getInitParams() {
        return initParams().entrySet().stream()
                .map(e -> InitParam.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    };

    @JsonIgnore
    @XmlTransient
    Map<String,String> initParams();



    static DotPortlet from(Portlet portlet) {
        return DotPortlet.builder()
                .portletId(portlet.getPortletId())
                .portletClass(portlet.getPortletClass())
                .initParams(portlet.getInitParams())
                .build();
    }

    default Portlet toPortlet() {
        return new Portlet(getPortletId(), getPortletClass(), initParams());
    }

    class Builder extends DotPortletBuilder implements XMLEnabledBuilder<DotPortlet> {
        //@XmlElementWrapper(name = "init-params")
        @XmlElement(name = "init-param")
        @JsonSetter("init-param")
        @JacksonXmlElementWrapper(useWrapping = false, localName = "init-params")
        public Builder addAllInitParams(List<InitParam> initParams) {
            putAllInitParams(initParams.stream()
                    .collect(Collectors.toMap(InitParam::getName, InitParam::getValue)));
            return this;
        }

        public Builder from(Portlet portlet) {
            return portletId(portlet.getPortletId())
                .portletClass(portlet.getPortletClass())
                .initParams(portlet.getInitParams());
        }
    }

    static Builder builder() {
        return new Builder();
    }
}