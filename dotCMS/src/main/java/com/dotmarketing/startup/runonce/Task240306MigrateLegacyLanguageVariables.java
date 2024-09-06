package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.languagevariable.business.ImmutableMigrationSummary;
import com.dotcms.languagevariable.business.LegacyLangVarMigrationHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static com.dotcms.languagevariable.business.LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME;

/**
 * Migrates legacy Language Variables to the new Contentlets of type {@code Language Variable}. This
 * task will take care of reading the Language Variables that live in properties files for each
 * language, and creates a new Contentlet of type {@code Language Variable} for each one of them.
 *
 * @author Fabrizzio Araya
 * @since Mar 15th, 2024
 */
public class Task240306MigrateLegacyLanguageVariables implements StartupTask {

    private ImmutableMigrationSummary migrationSummary = null;

    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * Checks if the {@code Language Variable} Content Type exists.
     *
     * @return An {@link Optional} with the inode of the {@code Language Variable} Content Type, or
     * an empty one.
     */
    private static Optional<String> langVarContentType() {
        final String inode = new DotConnect().setSQL(
                        "select inode from structure where velocity_var_name like ?")
                .addParam(LANGUAGEVARIABLE_VAR_NAME).getString(ContentTypeFactory.INODE_COLUMN);
        return Optional.ofNullable(inode);
    }

    /**
     * Checks if the {@code Language Variable} Content Type exists. If it doesn't, it creates it and
     * assigns the System Workflow to it so that the migrated Language Variables as Contents can be
     * created correctly.
     *
     * @return An {@link Optional} with the inode of the {@code Language Variable} Content Type.
     *
     * @throws DotDataException An error occurred creating the Content Type.
     */
    public Optional<String> checkContentType() throws DotDataException {
        final Optional<String> contentType = langVarContentType();
        if (contentType.isPresent()) {
            return contentType;
        }
        Logger.info(this, "Content type not found. Creating default language variable content type");
        new Task04210CreateDefaultLanguageVariable().executeUpgrade();
        Logger.info(this, "Updating the language variable content type");
        new Task240131UpdateLanguageVariableContentType().executeUpgrade();
        final Optional<String> langVarContentTypeIDOpt = langVarContentType();
        if (langVarContentTypeIDOpt.isPresent()) {
            this.assignSystemWorkflow(langVarContentTypeIDOpt.get());
            return langVarContentTypeIDOpt;
        }
        throw new DotRuntimeException("Failure creating or updating the Language Variable Content Type. Skipping migration.");
    }

    /**
     * Assigns the System Workflow to the {@code Language Variable} Content Type.
     *
     * @param langVarContentTypeID The inode of the {@code Language Variable} Content Type.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     */
    private void assignSystemWorkflow(final String langVarContentTypeID) throws DotDataException {
        final ContentType languageVariableContentType;
        try {
            languageVariableContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(langVarContentTypeID);
        } catch (final DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
        APILocator.getWorkflowAPI().saveSchemeIdsForContentType(languageVariableContentType,
                Set.of(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID));
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.info(this, "Migrating legacy Language Variables to Contentlets");
        final Optional<String> contentTypeId = checkContentType();
        Logger.info(this,String.format("Language Variable Content Type ID is '%s'", contentTypeId.get()));
        try {
            final Path path = LegacyLangVarMigrationHelper.messagesDir();
            Logger.info(this, String.format("Migrating legacy Language Variables in: '%s'", path));
            final LegacyLangVarMigrationHelper helper = new LegacyLangVarMigrationHelper(contentTypeId.get());
            this.migrationSummary = helper.migrateLegacyLanguageVariables(path);
            Logger.info(this,"===================================================");
            Logger.info(this,"  Legacy language variables migration completed!");
            Logger.info(this,"===================================================");
            Logger.info(this, "Migration Summary: " + migrationSummary.toString());
            
        } catch (final Exception e) {
            throw new DotRuntimeException(String.format("Error migrating legacy language variables: " +
                    "%s", ExceptionUtil.getErrorMessage(e)), e);
        }
    }

    /**
     * Returns the migration summary
     * @return the migration summary
     */
    public Optional<ImmutableMigrationSummary> getMigrationSummary() {
        return Optional.ofNullable(migrationSummary);
    }

}
