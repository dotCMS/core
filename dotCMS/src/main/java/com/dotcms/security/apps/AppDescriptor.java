package com.dotcms.security.apps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This is bean meant to read data from the input yaml file that describes the service
 * The file might look a bit like this:
 * name: "Slack"
 * description: Slack emerges as an internal tool used by the company Tiny Speck
 *
 * iconUrl: "/slackIcon.png"
 * allowExtraParameters:false
 * params:
 *  param1:
 *   value: "value-1"
 *   hidden: false
 *   type: "STRING"
 *   label: "label"
 *   hint: "hint"
 *  required: false
 *
 */
public interface AppDescriptor {

    /**
     * Service unique identifier
     * @return
     */
    String getKey();

    /**
     * Any name
     * @return
     */
    String getName();

    /**
     * Any meaningful read
     * @return
     */
    String getDescription();

    /**
     * an avatar URL
     * @return
     */
    String getIconUrl();

    /**
     * Tells the API if we allow any additional beside the ones already defined in the params map.
     * @return
     */
    boolean isAllowExtraParameters();

    /**
     * Tells the API if we allow any additional beside the ones already defined in the params map.
     * @return
     */
    Boolean getAllowExtraParameters();

    /**
     * Holds the definition of the params expected by the service.
     * This method returns a defensive copy.
     * @return
     */
    Map<String, ParamDescriptor> getParams();

}
