package com.dotmarketing.business.portal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(
        //overshadowImplementation = true,
        //typeBuilder = "*Builder",
        typeAbstract = "*Tuple",
        // Generate without any suffix, just raw detected name
        typeImmutable = "*",
        // Make generated public, leave underscored as package private
        visibility = ImplementationVisibility.PUBLIC,
        allParameters = true,
        defaults = @Value.Immutable(builder = false, copy = false)
)
@JsonSerialize(as = InitParam.class)
@JsonDeserialize(as = InitParam.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "portlet")
public interface InitParamTuple {


    @JacksonXmlProperty(localName = "name")
    String getName();

    @JacksonXmlProperty(localName = "value")
    String getValue();

}