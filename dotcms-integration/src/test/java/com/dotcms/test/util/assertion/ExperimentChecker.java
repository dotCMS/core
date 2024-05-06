package com.dotcms.test.util.assertion;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;

/**
 * {@link AssertionChecker} concrete class for {@link Experiment}
 */
public class ExperimentChecker implements AssertionChecker<Experiment>{

    @Override
    public Map<String, Object> getFileArguments(final Experiment experiment, final File file) {
         try {
            final Contentlet pageAsContent = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(experiment.pageId(), DEFAULT_VARIANT.name(), true);

            final HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(pageAsContent);

             final ExperimentVariant experimentNoDefaultVariant = experiment.trafficProportion().variants()
                     .stream()
                     .filter(experimentVariant -> !DEFAULT_VARIANT.name().equals(experimentVariant.id()))
                     .findFirst()
                     .orElseThrow();
             return Map.of(
                    "name", experiment.name(),
                    "description", experiment.description().orElseThrow(),
                    "id", experiment.id().orElseThrow(),
                    "page_url", htmlPageAsset.getURI(),
                    "no_default_variant_id", experimentNoDefaultVariant.id(),
                    "no_default_variant_name", experimentNoDefaultVariant.description(),
                        "page_id", htmlPageAsset.getIdentifier()

                    );
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/experiment/experiment.json";
    }

    @Override
    public File getFileInner(final Experiment experiment, File bundleRoot) {
        try {
            final Contentlet pageAsContentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(experiment.pageId());

            final Host host = APILocator.getHostAPI().find(pageAsContentlet.getHost(), APILocator.systemUser(), false);

            final String path = bundleRoot.getPath() + File.separator + "live" + File.separator +
                    host.getHostname() + File.separator + experiment.id().orElseThrow() + ".experiment.json";

            return new File(path);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Collection<String> getRegExToRemove(final File file) {
        return list(
                "\"creationDate\":.*,\"modDate\":.*"
        );
    }
}
