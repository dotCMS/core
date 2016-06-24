package com.dotmarketing.osgi;

import java.io.Serializable;

/**
 * Encapsulates an action forward.
 * @author jsanca
 */
public class ActionForwardBean implements Serializable {

    private final String name;
    private final String path;
    private final boolean redirect;

    /**
     * Constructor.
     * @param name key name for the forward
     * @param path path for the resource
     * @param redirect true if you want a redirect
     */
    public ActionForwardBean(final String name,
                             final String path,
                             final boolean redirect) {

        this.name = name;
        this.path = path;
        this.redirect = redirect;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isRedirect() {
        return redirect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionForwardBean that = (ActionForwardBean) o;

        if (redirect != that.redirect) return false;
        if (!name.equals(that.name)) return false;
        return path.equals(that.path);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + (redirect ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActionForwardBean{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", redirect=" + redirect +
                '}';
    }
} // E:O:F:ActionForwardBean.
