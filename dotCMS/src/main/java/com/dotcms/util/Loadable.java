package com.dotcms.util;

/**
 * This interface is intended to model a pre-loadable state behavior
 * The Loadable State behavior is intended to save in terms of having to do a recurrent effort required to change the state.
 */
public interface Loadable {

    /**
     * implement to ensure the state has been acquired after having called the `load` Method
     * @return
     */
    boolean isLoaded();

    /**
     * This method changes the state of the model and makes it hold values that require some effort.
     * Once this method has been called any additional call performed on this method should do nothing.
     * The state is loaded just once.
     */
    void load();

}
