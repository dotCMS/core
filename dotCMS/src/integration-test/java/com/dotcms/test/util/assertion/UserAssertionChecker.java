package com.dotcms.test.util.assertion;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.io.File;
import java.util.Map;

public class UserAssertionChecker implements AssertionChecker<User> {

    @Override
    public Map<String, Object> getFileArguments(User asset, File file) {

        return map(

        );
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/user/user.user.xml";
    }

    @Override
    public File getFileInner(final User user, final File bundleRoot) {
        String hostFilePath = bundleRoot.getPath() + File.separator
                + "users" + File.separator
                + user.getUserId() + ".user.xml";

        return new File(hostFilePath);
    }
}
