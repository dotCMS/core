package com.dotmarketing.portlets.contentlet.business;


import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.List;

/**
 * This delegate is used to destroy contentlets
 * Currently the only implementation is {@link ContentletDestroyDelegateImpl}
 * And it uses the {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#destroy(List, User, boolean)} method
 * But this delegate can be used to implement a different logic to destroy contentlets
 */
public interface ContentletDestroyDelegate {

    /**
     * We expect any implementation to destroy the contentlets to have a method like this
     * @param contentlets
     * @param user
     */
    void destroy(final List<Contentlet> contentlets, final User user);

    /**
     * instance getter
     * @return
     */
     static ContentletDestroyDelegate newInstance(){
         final String clazz = Config.getStringProperty("CONTENTLET_DESTROY_DELEGATE_CLASS",
                 ContentletDestroyDelegateImpl.class.getCanonicalName()
         );
         return  Try.of(() -> (ContentletDestroyDelegate) Class.forName(clazz).getDeclaredConstructor().newInstance())
                 .getOrElse(new ContentletDestroyDelegateImpl());
     }

}
