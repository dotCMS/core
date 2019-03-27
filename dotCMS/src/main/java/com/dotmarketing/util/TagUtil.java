package com.dotmarketing.util;

import com.dotcms.repackage.com.google.common.collect.Sets;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.tag.model.Tag;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TagUtil {

    /**
     * Method that converts a given {@link Tag} List to a String of tag names with a CSV format
     *
     * @param tags tags to convert
     * @return CSV string with the list of tag names
     */
    public static String tagListToString(List<Tag> tags) {

        //Now we need to use the found tags in order to accrue them each time this page is visited
        if ( tags != null && !tags.isEmpty() ) {

            StringBuilder tagsPlainList = new StringBuilder();
            Iterator<Tag> tagsIterator = tags.iterator();
            while ( tagsIterator.hasNext() ) {

                Tag tag = tagsIterator.next();
                tagsPlainList.append(tag.getTagName());

                if ( tagsIterator.hasNext() ) {
                    tagsPlainList.append(",");
                }
            }

            return tagsPlainList.toString();
        }

        return "";
    }

    /**
     * Method that accrues a given {@link Tag} List to the current {@link Visitor}
     *
     * @param request HttpServletRequest object required in order to find the current {@link Visitor}
     * @param tags    {@link Tag} list to accrue
     */
    public static void accrueTags(HttpServletRequest request, List<Tag> tags) {

        //Now we need to use the found tags in order to accrue them each time this page is visited
        if ( tags != null && !tags.isEmpty() ) {
            accrueTags(request, tagListToString(tags));
        }
    }

    /**
     * Method that accrues a given String of tag names with a CSV format to the current {@link Visitor}
     *
     * @param request HttpServletRequest object required in order to find the current {@link Visitor}
     * @param tags    String of tag names with a CSV format to accrue
     */
    public static void accrueTags(HttpServletRequest request, String tags) {

        if ( !UtilMethods.isSet(tags) ) {
            return;
        }

        //Getting the current visitor
        Optional<Visitor> opt = APILocator.getVisitorAPI().getVisitor(request);

        //Validate the visitor exist
        if ( !opt.isPresent() ) {
            return;
        }

        Visitor visitor = opt.get();
        //Accrue the found tags
        accrueTagsToVisitor(visitor, tags);
    }

    /**
     * Method that accrues a given String of tag names with a CSV format to the given {@link Visitor}
     *
     * @param visitor {@link Visitor} to accrue the given tags
     * @param tags    String of tag names with a CSV format to accrue
     */
    public static void accrueTagsToVisitor(Visitor visitor, String tags) {

        if ( !UtilMethods.isSet(tags) ) {
            return;
        }

        String[] foundTags = tags.split(",");
        Set<String> tagsSet = Sets.newHashSet(foundTags);

        //Accrue the found tags
        visitor.addAccruedTags(tagsSet);
    }

}
