package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.contenttype.model.type.ContentType;

/**
 * This class is used to store the content type that is being deleted in a thread local variable.
 */
public class ContentletDestroyThreadLocal {

    private static final ThreadLocal<Boolean> threadLocal = new ThreadLocal<>();

    /**
     * This method is used to set the content type that is being deleted in a thread local variable.
     * @param type
     */
    public void set(final Boolean flag) {
        threadLocal.set(flag);
    }

    /**
     * This method is used to get the content type that is being deleted in a thread local variable.
     * @return
     */
    public Boolean get() {
        return threadLocal.get();
    }

    /**
     * This method is used to remove the content type that is being deleted in a thread local variable.
     */
    public  void remove() {
        threadLocal.remove();
    }

    /**
     * This method is used to get the instance of the DeletionThreadLocal class.
     * @return
     */
    public enum INSTANCE {
        INSTANCE;
        private final ContentletDestroyThreadLocal helper = new ContentletDestroyThreadLocal();

        public static ContentletDestroyThreadLocal get() {
            return INSTANCE.helper;
        }

    }

}
