package com.dotcms.integritycheckers;

import com.dotcms.LicenseTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import io.vavr.Lazy;
import java.util.UUID;

public interface IntegrityCheckerTest {

    Lazy<String> endpointId = Lazy.of(() -> UUID.randomUUID().toString());

    /**
     * Simply validate we have a non-null contentlet_as_json
     * @param newIdentifier
     * @return
     * @throws DotDataException
     */
    default boolean validateFix(final String newIdentifier) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(
                "SELECT COUNT(*) as x FROM contentlet c WHERE c.identifier = ? AND c.contentlet_as_json is not null ");
        dotConnect.addParam(newIdentifier);
        return dotConnect.getInt("x") == 1;
    }

    default void setUpEnvironment() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

}
