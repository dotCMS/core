package com.dotmarketing.startup.runonce;

import static com.dotcms.util.ConversionUtils.toLong;
import static com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryImpl.DEFAULT_LANGUAGE_CODE;
import static com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryImpl.DEFAULT_LANGUAGE_COUNTRY_CODE;
import static com.dotmarketing.util.ConfigUtils.*;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.sql.SQLException;
import java.util.Map;

public class Task210908AddCompanyDefaultLanguage implements StartupTask {

    static final String POSTGRES_SCRIPT = "ALTER TABLE company ADD COLUMN default_language_id int8 null;";
    static final String MYSQL_SCRIPT = "ALTER TABLE company ADD default_language_id bigint null;";
    static final String ORACLE_SCRIPT = "ALTER TABLE company ADD default_language_id number(19,0) null;";
    static final String MSSQL_SCRIPT = "ALTER TABLE company ADD default_language_id default_language_id null;";
    static final String SELECT = "SELECT default_language_id FROM company WHERE companyid = ? ";
    static final String UPDATE = "UPDATE company SET default_language_id = ? WHERE companyid = ? ";
    static final String DEFAULT_COMPANY_ID = "dotcms.org";
    static final String SELECT_LANGUAGE_BY_LANG_AND_COUNTRY_CODES = "select * from language where lower(language_code) = ? and lower(country_code) = ?";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final DotConnect dotConnect = new DotConnect();
        if (!companyHasDefaultLanguageColumn()) {
            final String columnScript = getAddColumnScript();
            dotConnect.setSQL(columnScript);
            dotConnect.loadResult();
        }

        insertDefaultLanguageIfNecessary(dotConnect);
    }

    private String getAddColumnScript() {
        String sqlScript = null;
        if (DbConnectionFactory.isPostgres()) {
            sqlScript = POSTGRES_SCRIPT;
        } else if (DbConnectionFactory.isMySql()) {
            sqlScript = MYSQL_SCRIPT;
        } else if (DbConnectionFactory.isOracle()) {
            sqlScript = ORACLE_SCRIPT;
        } else if (DbConnectionFactory.isMsSql()) {
            sqlScript = MSSQL_SCRIPT;
        }
        return sqlScript;
    }

    public boolean companyHasDefaultLanguageColumn() {
        return Try.of(() -> new DotDatabaseMetaData().hasColumn("company", "default_language_id"))
                .getOrElseThrow(DotRuntimeException::new);
    }

    private void insertDefaultLanguageIfNecessary(final DotConnect dotConnect)
            throws DotDataException {

        final String defaultLanguageId = dotConnect.setSQL(SELECT).addParam(DEFAULT_COMPANY_ID)
                .getString("default_language_id");
        if (UtilMethods.isSet(defaultLanguageId)) {
            Logger.info(Task210908AddCompanyDefaultLanguage.class, String.format("Company already had a default language [%s] set.", defaultLanguageId));
        } else {

            final Tuple2<String, String> defaultLanguageDeclaration = getDeclaredDefaultLanguage();

            final String langCode = defaultLanguageDeclaration._1;
            final String countryCode = defaultLanguageDeclaration._2;

            if (UtilMethods.isNotSet(langCode) || UtilMethods.isNotSet(countryCode)) {
                Logger.warn(Task210908AddCompanyDefaultLanguage.class,
                        "Unable to find a default Language specification in the properties file. No default Language will be written into te company table.");
                return;
            }

            final Map<String, Object> map = dotConnect
                    .setSQL(SELECT_LANGUAGE_BY_LANG_AND_COUNTRY_CODES)
                    .addParam(langCode)
                    .addParam(countryCode)
                    .loadObjectResults()
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (null == map) {
                //The Language must be already inserted so it can be referenced from the company table.
                //Otherwise we'd be opening a doorway for invalid language creation.
                throw new DotDataException(String.format(
                        "Invalid attempt to set a non-existing language (%s,%s) as a default language. Make sure the default has been inserted first into the Language table.",
                        langCode, countryCode));

            } else {
                //Use the existing Language coming from the Language Table.
                final long existingLangId = toLong(map.get("id"), 0L);
                dotConnect.setSQL(UPDATE)
                        .addParam(existingLangId)
                        .addParam(DEFAULT_COMPANY_ID)
                        .loadResult();
            }
        }
    }




}
