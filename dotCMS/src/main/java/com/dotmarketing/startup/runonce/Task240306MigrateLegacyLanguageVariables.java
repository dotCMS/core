package com.dotmarketing.startup.runonce;

import static com.dotcms.languagevariable.business.LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME;

import com.dotcms.languagevariable.business.ImmutableMigrationSummary;
import com.dotcms.languagevariable.business.LegacyLangVarMigrationHelper;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Migrates legacy language variables to the new language variable content type
 */
public class Task240306MigrateLegacyLanguageVariables implements StartupTask {

    private ImmutableMigrationSummary migrationSummary = null;

    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * Checks if the language variable content type exists
     * @return the inode of the language variable content type
     */
    public static Optional<String> langVarContentType() {
        final String inode = new DotConnect().setSQL(
                        "select inode from structure where velocity_var_name like ?")
                .addParam(LANGUAGEVARIABLE_VAR_NAME).getString("inode");
        return Optional.ofNullable(inode);
    }

    /**
     * Checks if the language variable content type exists, if not, it creates it
     * @return the inode of the language variable content type
     * @throws DotDataException if there is an error creating the content type
     */
    Optional<String> checkContentType() throws DotDataException {
        Optional<String> contentType = langVarContentType();
        if (contentType.isPresent()) {
            return contentType;
        }
        //The ContentType is not found, so we need to create it
        Logger.info(this, "Content type not found. Creating default language variable content type");
        new Task04210CreateDefaultLanguageVariable().executeUpgrade();
        Logger.info(this, "Updating language variable content type ");
        new Task240131UpdateLanguageVariableContentType().executeUpgrade();
        return langVarContentType();
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.info(this, "Migrating legacy language variables");
        final Optional<String> contentTypeInode = checkContentType();
        if (contentTypeInode.isEmpty()) {
            throw new DotRuntimeException("Failure Creating or Updating Language Variable Content Type. Skipping migration.");
        }
        Logger.info(this,"Language Variable ContentType inode is " + contentTypeInode.get());
        try {
            final Path path = LegacyLangVarMigrationHelper.messagesDir();
            Logger.info(this, "Migrating legacy language variables from " + path);
            final LegacyLangVarMigrationHelper helper = new LegacyLangVarMigrationHelper(contentTypeInode.get());
            this.migrationSummary = helper.migrateLegacyLanguageVariables(path);
            Logger.info(this, "Legacy language variables migration completed");
            Logger.info(this, "Migration Summary: " + migrationSummary.toString());
            
        } catch (Exception e) {
            throw new DotRuntimeException("Error migrating legacy language variables", e);
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
