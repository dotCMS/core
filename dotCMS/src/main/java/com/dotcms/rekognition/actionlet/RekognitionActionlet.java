package com.dotcms.rekognition.actionlet;


import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.rekognition.api.RekognitionAPI;
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
    public static final String AMAZON_REKOGNITION_APP_CONFIG_KEY = "dotAmazonRekognition-config";
    public static final String ACCESS_KEY_VAR = "accessKey";
    public static final String SECRET_ACCESS_KEY_VAR = "secretAccessKey";
    public static final String MAX_LABELS_VAR = "maxLabels";
    public static final String MIN_CONFIDENCE_VAR = "minConfidence";

    private final TagAPI tagAPI = APILocator.getTagAPI();

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return null;
    }

    @Override
    public String getName() {
        return "Auto Tag Images - AWS";
    }

    @Override
    public String getHowTo() {
        return "This Actionlet will automatically tag images using Amazon's Rekognition AI engine. This actionlet needs be after the Save Actionlet";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        Optional<Field> tagFieldOpt = null;
        Optional<File> imageOpt     = null;
        Optional<AppSecrets> appSecrets = null;
        File image;
        List<Field> fields;
        try {

            fields = APILocator.getContentTypeAPI(processor.getUser()).find(contentlet.getContentTypeId()).fields();
            tagFieldOpt = fields.stream().filter(TagField.class::isInstance).findFirst();
            //Check if there is a Tag Field, if not there is nothing to do here
            if (!tagFieldOpt.isPresent()) {
                Logger.warn(this,"There is no Tag Field in the Content Type");
                return;
            }

            final List<Tag> tags = tagAPI.getTagsByInode(contentlet.getInode());
            //Check if there is already a Tag with the name TAGGED_BY_AWS, if there is Tags were already generated and nothing to do here
            if (tags.stream().anyMatch(tag -> TAGGED_BY_AWS.equalsIgnoreCase(tag.getTagName()))) {
                Logger.warn(this,"Tags already generated");
                return;
            }

            imageOpt = fields.stream().filter(BinaryField.class::isInstance)
                    .map(field -> Try.of(()->contentlet.getBinary(field.variable())).getOrNull())
                    .filter(img -> null != img && UtilMethods.isImage(img.getAbsolutePath()))
                    .findFirst();

            //Check if there is a Binary Field and if there is an Image set in it, if not there is nothing to do here
            if (!imageOpt.isPresent()) {
                Logger.warn(this,"There is no Binary Field or an Image is not set in it");
                return;
            }
            image = imageOpt.get();

            //Get Values from Secrets
            final Host host = Try.of(() -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(),false)).getOrElse(APILocator.systemHost());
            appSecrets = APILocator.getAppsAPI().getSecrets(AMAZON_REKOGNITION_APP_CONFIG_KEY,true,host,APILocator.systemUser());

            if(!appSecrets.isPresent()) {
                Logger.warn(RekognitionActionlet.class,"There is no config set, please set it via Apps Tool");
                return;
            }

            final String accessKey = appSecrets.get().getSecrets().get(ACCESS_KEY_VAR).getString();
            final String secretAccessKey = appSecrets.get().getSecrets().get(SECRET_ACCESS_KEY_VAR).getString();
            final int maxLabels = Integer.parseInt(appSecrets.get().getSecrets().get(MAX_LABELS_VAR).getString());
            final float minConfidence = Float.parseFloat(appSecrets.get().getSecrets().get(MIN_CONFIDENCE_VAR).getString());
            //End Get Values from Secrets

            if (image.length() > IMAGE_MAX_LENGTH) {

                final Map<String, String[]> args = new HashMap<>();
                args.put("resize_w", new String[]{"1000"});
                image = new ResizeImageFilter().runFilter(image, args);
            }

            final List<String> awsTags = new RekognitionAPI(accessKey,secretAccessKey).generateTags(image, maxLabels, minConfidence);

            Logger.debug(this,"Tags generated by AWS: " + awsTags.toString());

            awsTags.add(TAGGED_BY_AWS);
            for (final String tag : awsTags) {
                tagAPI.addContentletTagInode(tag, contentlet.getInode(), contentlet.getHost(), tagFieldOpt.get().variable());
            }

            HibernateUtil.addCommitListener(contentlet.getInode(),
                    ()->this.refresh(contentlet));

            CacheLocator.getContentletCache().remove(contentlet);
        } catch (Exception e) {
            Logger.warnAndDebug(RekognitionActionlet.class, "Unable to autogenerate the rekognition tags: "
                    + e.getMessage(), e);
        } finally {
            if(UtilMethods.isSet(appSecrets) && appSecrets.isPresent()){
                appSecrets.get().destroy();
            }
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
