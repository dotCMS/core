package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class UniqueFieldDataBaseUtilTake2  {

    public static class Target {
        boolean uniquePerSite;
        Set<String> fieldIds;
        String contentTypeId;

        public Target(String contentTypeId, Set<String> fieldIds, boolean uniquePerSite) {
            this.contentTypeId = contentTypeId;
            this.fieldIds = fieldIds;
            this.uniquePerSite = uniquePerSite;
        }

        @Override
        public String toString() {
            return "Target{" +
                    "contentTypeId='" + contentTypeId + '\'' +
                    ", fieldIds=" + fieldIds +
                    ", uniquePerSite=" + uniquePerSite +
                    '}';
        }
    }

    public void run () throws Exception {

        final Set<Target> targets = this.getTargets();
        final DBUniqueFieldValidationStrategy strategy = CDIUtils.getBeanThrows(DBUniqueFieldValidationStrategy.class);
        final DotConcurrentFactory.SubmitterConfig config = new DotConcurrentFactory.SubmitterConfigBuilder()
                .poolSize(2)
                .maxPoolSize(2)
                .keepAliveMillis(1000)
                .queueCapacity(10000)
                .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .build();

        final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter("UniqueFieldSubmitter", config);
        for (final Target target : targets) {

            final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(target.contentTypeId);

            Logger.debug(this, ()-> "Migrating the content type: " + contentType.variable());
            submitter.submit(()-> {

                migrateContentType(target, contentType, strategy);
            });
        }
    }

    private void migrateContentType(final Target target,
                                           final ContentType contentType, final DBUniqueFieldValidationStrategy strategy) {

        try {

            final Set<Field> fields = target.fieldIds.stream().map(fieldId -> Try.of(() -> APILocator.getContentTypeFieldAPI().find(fieldId))
                    .getOrNull()).filter(Objects::nonNull).collect(Collectors.toSet());
            int limit = Config.getIntProperty("UNIQUE_FIELDS_MIGRATION_LIMIT", 100);
            int offset = 0;
            List<Contentlet> contentlets = APILocator.getContentletAPI().findByStructure(
                    contentType.id(), APILocator.systemUser(), false, limit, offset);
            while (UtilMethods.isSet(contentlets)) {
                for (final Contentlet contentlet : contentlets) {

                    for (final Field field : fields) {

                        final Object fieldValue = contentlet.get(field.variable());
                        try {

                            Logger.info(this, "Validating contentlet: " + contentlet.getIdentifier() + "on field: " + field.variable()
                                    + " with value: " + fieldValue + " in content type: " + contentType.variable());
                            strategy.innerValidate(contentlet, field, fieldValue, contentType);
                        } catch (UniqueFieldValueDuplicatedException e) {
                            Logger.info(this, "Duplicated value found for contentlet: " + contentlet.getIdentifier() + "on field: " + field.variable()
                             + " with value: " + fieldValue + " in content type: " + contentType.variable());
                        } catch (DotDataException | DotSecurityException e) {
                            Logger.error(this, e.getMessage(), e);
                        }
                    }
                }

                Logger.info(this, ()-> "Sleeping for: " +
                        Config.getIntProperty("UNIQUE_FIELDS_MIGRATION_SLEEP", 1000) + " ms");
                ThreadUtils.sleep(Config.getIntProperty("UNIQUE_FIELDS_MIGRATION_SLEEP", 1000));
                offset += limit;
                contentlets = APILocator.getContentletAPI().findByStructure(
                        contentType.id(), APILocator.systemUser(), false, limit, offset);
            }
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }


    private Set<Target> getTargets() throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT " +
                        "    f.inode, " +
                        "    f.structure_inode, " +
                        "    MAX(CASE " +
                        "            WHEN fv.variable_key = 'uniquePerSite' AND fv.variable_value = 'true' " +
                        "            THEN 1 " +
                        "            ELSE 0 " +
                        "        END) AS is_unique_per_site " +
                        "FROM field f " +
                        "LEFT JOIN field_variable fv ON fv.field_id = f.inode  " +
                        "WHERE f.unique_ = true " +
                        "GROUP BY f.inode, f.structure_inode;")
                .loadObjectResults();

        // Group by contentTypeId
        final Map<String, Target> groupedTargets = new HashMap<>();

        for (Map<String, Object> row : results) {
            String contentTypeId = (String) row.get("structure_inode");
            String fieldId = (String) row.get("inode");
            boolean uniquePerSite = ConversionUtils.toBooleanFromDb(row.get("is_unique_per_site"));

            groupedTargets.computeIfAbsent(contentTypeId, k -> new Target(contentTypeId, new HashSet<>(), uniquePerSite))
                    .fieldIds.add(fieldId);
        }

        return new HashSet<>(groupedTargets.values());
    }


    public void loadContentsWithLimit(int limit){

    }

}
