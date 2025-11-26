package com.dotmarketing.portlets.workflows.actionlet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Encapsulates configuration for an Actionlet
 * @author jsanca
 */
@Target({ ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Actionlet {

    /**
     * Set this to true if the actionlet saves a content
     */
    boolean save()    default false;

    /**
     * Set this to true if the actionlet publish a content
     */
    boolean publish() default false;

    /**
     * Set this to true if the actionlet unpublish a content
     */
    boolean unpublish() default false;

    /**
     * Set this to true if the actionlet archive a content
     */
    boolean archive() default false;

    /**
     * Set this to true if the actionlet unarchive a content
     */
    boolean unarchive() default false;

    /**
     * Set this to true if the actionlet deletes a content
     */
    boolean delete() default false;

    /**
     * Set this to true if the actionlet destroy (delete all versions) a content
     */
    boolean destroy() default false;

    /**
     * Set this to true if the actionlet push publish a content
     */
    boolean pushPublish() default false;

    /**
     * Set this to true if the actionlet is runnable only on batches
     */
    boolean onlyBatch() default false;

    /**
     * Set this to true if the actionlet is commentable
     */
    boolean comment() default false;

    /**
     * Set this to true if the sub actionlet is a reset content
     * @return
     */
    boolean reset() default false;
}
