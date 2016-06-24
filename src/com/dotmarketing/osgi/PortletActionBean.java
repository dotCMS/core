package com.dotmarketing.osgi;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Encapsulates the info for a Porlet Action Bean
 * @author jsanca
 */
public class PortletActionBean implements Serializable {

    private final String path;
    private final String type;
    private final List<ActionForwardBean> actionForwards;
    private final String [] portletXmls;


    public PortletActionBean(final String path,
                             final String type,
                             final List<ActionForwardBean> actionForwards,
                             final String[] portletXmls) {

        this.path = path;
        this.type = type;
        this.actionForwards = actionForwards;
        this.portletXmls = portletXmls;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public List<ActionForwardBean> getActionForwards() {
        return actionForwards;
    }

    public String[] getPortletXmls() {
        return portletXmls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PortletActionBean that = (PortletActionBean) o;

        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (actionForwards != null ? !actionForwards.equals(that.actionForwards) : that.actionForwards != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(portletXmls, that.portletXmls);

    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (actionForwards != null ? actionForwards.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(portletXmls);
        return result;
    }

    @Override
    public String toString() {
        return "PortletActionBean{" +
                "path='" + path + '\'' +
                ", type='" + type + '\'' +
                ", actionForwards=" + actionForwards +
                ", portletXmls=" + Arrays.toString(portletXmls) +
                '}';
    }
} // E:O:F:PortletActionBean.
