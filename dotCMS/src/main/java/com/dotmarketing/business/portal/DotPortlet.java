package com.dotmarketing.business.portal;

import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Portlet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements the JAXB mapping for the portlet definitions in dotCMS.
 *
 * @author Jose Castro
 * @since Jun 17th, 2024
 */
@XmlRootElement(name = "portlet")
public class DotPortlet extends Portlet {

    @XmlElement(name = "init-param")
    private transient List<InitParam> initParamsAsList;

    /**
     * Default class constructor, required by JAXB.
     */
    @SuppressWarnings("unused")
    public DotPortlet() {
        super(null, null, new HashMap<>());
    }

    /**
     * Creates an instance of this class based on the original/legacy definition or a dotCMS
     * Portlet.
     *
     * @param portlet The original {@link Portlet} object.
     */
    public DotPortlet(final Portlet portlet) {
        this(portlet.getPortletId(), portlet.getPortletClass(), portlet.getInitParams());
    }

    /**
     * Creates an instance of this class.
     *
     * @param portletId    The ID of the portlet.
     * @param portletClass The base class that handles the existing legacy Liferay/Struts logic to
     *                     render the portlet.
     * @param initParams   The initialization and/or configuration parameters for the portlet.
     */
  public DotPortlet(String portletId,  String portletClass, Map<String, String> initParams) {
    super(portletId, portletClass, initParams);
        this.initParamsAsList = new ArrayList<>();
        for (Map.Entry<String, String> entry : initParams.entrySet()) {
            this.initParamsAsList.add(new InitParam(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    @XmlElement(name = "portlet-name")
    public String getPortletId() {
        return portletId;
    }

    public void setPortletId(final String portletId) {
        super.portletId = portletId;
    }

    @Override
    @XmlElement(name = "portlet-class")
    public String getPortletClass() {
        return portletClass;
    }

    public void setPortletClass(final String portletClass) {
        super.portletClass = portletClass;
    }

    @XmlElement(name = "init-param")
    public List<InitParam> getInitParamList() {
        return this.initParamsAsList;
    }

    /**
     * Returns the initialization parameters of the portlet as a map. By default, the JAXB mapping
     * will read them as a list of attributes, but, for backward compatibility, we need to return
     * them as mapped values.
     *
     * @return A map with the initialization parameters of the portlet.
     */
    @Override
    public Map<String, String> getInitParams() {
        if (UtilMethods.isSet(this.initParamsAsList)) {
            for (final InitParam initParam : this.initParamsAsList) {
                super.initParams.put(initParam.getName(), initParam.getValue());
            }
        }
        return super.getInitParams();
    }

    @Override
    public String toString() {
        return "DotPortlet{" +
                " portletId='" + this.portletId + '\'' +
                ", portletClass='" + this.portletClass + '\'' +
                ", portletSource='" + this.portletSource + '\'' +
                ", initParamsAsList=" + this.initParamsAsList +
                '}';
    }

    /**
     * Represents the initialization parameter tags in a Portlet definition. It can contain any
     * value/pair required by the Portlet, there's no technical limitation.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InitParam {

        @XmlElement(name = "name")
        private String name;

        @XmlElement(name = "value")
        private String value;

        /**
         * Default class constructor, required by JAXB.
         */
        @SuppressWarnings("unused")
        public InitParam() {
        }

        /**
         * Creates an instance of an initialization parameter.
         *
         * @param name  The name of the parameter.
         * @param value The value of the parameter.
         */
        public InitParam(final String name, final String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Returns the name of the parameter.
         *
         * @return The name of the parameter.
         */
        public String getName() {
            return this.name;
        }

        /**
         * Sets the name of the parameter.
         *
         * @param name The name of the parameter.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Returns the value of the parameter.
         *
         * @return The value of the parameter.
         */
        public String getValue() {
            return this.value;
        }

        /**
         * Sets the value of the parameter.
         *
         * @param value The value of the parameter.
         */
        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "InitParam{" +
                    "name='" + this.name + '\'' +
                    ", value='" + this.value + '\'' +
                    '}';
        }

  }

}
