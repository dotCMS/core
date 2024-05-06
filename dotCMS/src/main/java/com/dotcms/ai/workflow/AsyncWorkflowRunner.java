package com.dotcms.ai.workflow;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

interface AsyncWorkflowRunner extends Runnable, Serializable {


    long getRunAt();

    String getIdentifier();

    long getLanguage();

    void runInternal();

    default void run() {
        throw new DotRuntimeException("not implemented");
    }



    default void saveContentlet(Contentlet workingContentlet, User user) {
        try {

            workingContentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
            workingContentlet.setProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION, Boolean.TRUE);
            workingContentlet.setProperty(Contentlet.DONT_VALIDATE_ME, Boolean.TRUE);

            boolean isPublished = APILocator.getVersionableAPI().isLive(workingContentlet);
            workingContentlet = APILocator.getContentletAPI().checkin(workingContentlet, user, false);
            if (isPublished) {
                workingContentlet.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);
                workingContentlet.setProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION, Boolean.TRUE);
                workingContentlet.setProperty(Contentlet.DONT_VALIDATE_ME, Boolean.TRUE);
                if(validateContentlet(workingContentlet,user)) {
                    APILocator.getContentletAPI().publish(workingContentlet, user, false);
                }

            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

    }

    private boolean validateContentlet(Contentlet contentlet, User user){
        try {
            ContentletRelationships relationships = APILocator.getContentletAPI().getAllRelationships(contentlet);
            List<Category> categories = APILocator.getCategoryAPI().getParents(contentlet, user, false);
            APILocator.getContentletAPI().validateContentlet(contentlet, relationships, categories);
        }catch(Exception ve){
            return false;
        }
        return true;
    }



    default Contentlet checkoutLatest(String identifier, long language, User user) {

        Contentlet latest = getLatest(identifier, language, user);
        return Try.of(() -> APILocator.getContentletAPI().checkout(latest.getInode(), user, false)).getOrElseThrow(DotRuntimeException::new);

    }

    default Contentlet getLatest(String identifier, long language, User user) {
        Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI().getContentletVersionInfo(identifier, language);
        if (info.isEmpty() || UtilMethods.isEmpty(() -> info.get().getWorkingInode())) {
            throw new DotRuntimeException("unable to find content version info for id:" + identifier + " lang:" + language);
        }
        return Try.of(() -> APILocator.getContentletAPI().find(info.get().getWorkingInode(), user, false))
                .getOrElseThrow(DotRuntimeException::new);
    }
}
