package com.dotmarketing.portlets.contentlet.business;


import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.List;

public interface ContentletDestroyDelegate {

    void destroy(final List<Contentlet> contentlets, final User user);

     static ContentletDestroyDelegate newInstance(){
         final String clazz = Config.getStringProperty("CONTENTLET_DESTROY_DELEGATE_CLASS",
                 ContentletDestroyAPIImpl.class.getCanonicalName()
                // ContentletDestroyDelegateImpl.class.getCanonicalName()
         );
         return  Try.of(() -> (ContentletDestroyDelegate) Class.forName(clazz).getDeclaredConstructor().newInstance())
                 .getOrElse(new ContentletDestroyAPIImpl());
     }

}
