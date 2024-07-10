package com.dotmarketing.business.portal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * This class implements the JAXB mapping for the portlet definitions in dotCMS. It takes every
 * single portlet definition and maps it to a list of {@link DotPortlet} objects.
 *
 * @author Jose Castro
 * @since Jun 17th, 2024
 */
@XmlRootElement(name = "portlet-app")
public class PortletList {

    private List<DotPortlet> portlets;

    /**
     * Returns the list of portlets from a given XML file or Input Stream.
     *
     * @return The list of portlets.
     */
    @XmlElement(name = "portlet")
    public List<DotPortlet> getPortlets() {
        return portlets;
    }

    /**
     * Sets the list of portlets.
     *
     * @param portlets The list of portlets.
     */
    public void setPortlets(final List<DotPortlet> portlets) {
        this.portlets = portlets;
    }

}
