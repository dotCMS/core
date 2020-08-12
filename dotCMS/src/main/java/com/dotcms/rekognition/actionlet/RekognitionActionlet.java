package com.dotcms.rekognition.actionlet;


import com.dotmarketing.util.Config;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.rekognition.api.RekognitionApi;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.image.filter.ResizeImageFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

public class RekognitionActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    private final static int IMAGE_MAX_LENGTH = 5242879;
    private final static String TAGGED_BY_AWS = "TAGGED_BY_AWS";

    private final TagAPI tagAPI = APILocator.getTagAPI();

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<WorkflowActionletParameter>();

        params.add(new WorkflowActionletParameter("maxLabels", "Max Labels", Config.getStringProperty("max.labels", "15"), true));
        params.add(new WorkflowActionletParameter("minConfidence", "Minimum Confidence (percent)", Config.getStringProperty("min.confidence", "75"), true));
        return params;
    }

    @Override
    public String getName() {
        return "Auto Tag Images - AWS";
    }

    @Override
    public String getHowTo() {
        return "Max Labels is the maximum number of labels you are looking to return and Minimum Confidence is the minimum confidence level you will accept as valid tags";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        Optional<Field> tagFieldOpt = null;
        Optional<File> imageOpt     = null;
        Field tagField = null;
        File image     = null;

        List<Field> fields;
        try {

            fields = APILocator.getContentTypeAPI(processor.getUser()).find(contentlet.getContentTypeId()).fields();
            tagFieldOpt = fields.stream().filter(TagField.class::isInstance).findFirst();

            if (!tagFieldOpt.isPresent()) {
                return;
            }

            tagField = tagFieldOpt.get();

            final List<Tag> tags = tagAPI.getTagsByInode(contentlet.getInode());
            if (tags.stream().anyMatch(tag -> TAGGED_BY_AWS.equalsIgnoreCase(tag.getTagName()))) {
                return; // tags were already generated.
            }

            imageOpt = fields.stream().filter(BinaryField.class::isInstance)
                    .map(field -> Try.of(()->contentlet.getBinary(field.variable())).getOrNull())
                    .filter(img -> null != img && UtilMethods.isImage(img.getAbsolutePath()))
                    .findFirst();

            if (!imageOpt.isPresent()) {
                return;
            }

            // todo: Errick check the app secret
            final float minConfidence  = Float.parseFloat(params.get("minConfidence").getValue());
            // todo: Errick check the app secret
            final int maxLabels        = Integer.parseInt(params.get("maxLabels").getValue());

            if (image.length() > IMAGE_MAX_LENGTH) {

                final Map<String, String[]> args = new HashMap<>();
                args.put("resize_w", new String[]{"1000"});
                image = new ResizeImageFilter().runFilter(image, args);
            }

            final List<String> awsTags = new RekognitionApi().detectLabels(image, maxLabels, minConfidence);

            awsTags.add(TAGGED_BY_AWS);
            for (String tag : awsTags) {
                tagAPI.addContentletTagInode(tag, contentlet.getInode(), contentlet.getHost(), tagField.variable());
            }

            HibernateUtil.addCommitListener(contentlet.getInode(),
                    ()->this.refresh(contentlet));
        } catch (Exception e) {

            Logger.error(this, "Unable to autogenerate the rekognition tags: "
                    + e.getMessage(), e);
        }
    }

    private void refresh (final Contentlet contentlet) {

        try {

            APILocator.getContentletAPI().refresh(contentlet);
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

}
