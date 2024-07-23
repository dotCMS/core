package com.dotmarketing.business.portal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Tuple",
        typeImmutable = "*",
        visibility = ImplementationVisibility.PUBLIC,
        allParameters = true,
        defaults = @Value.Immutable(builder = false, copy = false)
)
@JsonSerialize(as = InitParam.class)
@JsonDeserialize(as = InitParam.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "init-param")
@XmlAccessorType(XmlAccessType.FIELD)
public interface InitParamTuple {

    @XmlElement(name = "name")
    @JsonProperty("name")
    String getName();

    @XmlElement(name = "value")
    @JsonProperty("value")
    String getValue();
}