package com.dotcms.test.util.assertion;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.rules.model.Rule;
import com.liferay.portal.model.User;

import java.io.File;
import java.util.Map;

/**
 * {@link AssertionChecker} concrete class for {@link Rule}
 */
public class RuleAssertionChecker implements AssertionChecker<Rule> {
    @Override
    public Map<String, Object> getFileArguments(Rule rule, File file) {
        return Map.of(
            "id", rule.getId(),
            "name", rule.getName(),
            "parent", rule.getParent()
        );
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/rule/rule.rule.xml";
    }

    @Override
    public File getFileInner(final Rule rule, final File bundleRoot) {
        final User systemUser = APILocator.systemUser();
        try {
            Host host = APILocator.getHostAPI().find(rule.getParent(), systemUser, false);

            if (host == null) {
                final Contentlet contentletByIdentifierAnyLanguage = APILocator.getContentletAPI()
                        .findContentletByIdentifierAnyLanguage(rule.getParent());

                host = APILocator.getHostAPI().find(contentletByIdentifierAnyLanguage.getHost(),
                        systemUser, false);
            }

            final String urlFilePath = bundleRoot.getPath() + File.separator + "live" + File.separator + host.getHostname()
                    + File.separator + rule.getId() + ".rule.xml";
            return new File(urlFilePath);

        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException();
        }

    }

}
