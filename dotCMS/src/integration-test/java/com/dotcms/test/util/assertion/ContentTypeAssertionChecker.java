package com.dotcms.test.util.assertion;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableKeyValueContentType;
import com.dotcms.enterprise.publishing.remote.bundler.FileBundlerTestUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * {@link AssertionChecker} concrete class for {@link ContentType}
 */
public class ContentTypeAssertionChecker implements AssertionChecker<ContentType> {

    @Override
    public Map<String, Object> getFileArguments(final ContentType contentType, File file) {

        try {
            final Host host = APILocator.getHostAPI().find(contentType.host(), APILocator.systemUser(), false);

            final List<WorkflowScheme> workflowSchemes = APILocator.getWorkflowAPI().findSchemesForContentType(contentType);

            Map<String, Object> arguments = map(
                    "content_type_name", contentType.name(),
                    "content_type_description", contentType.description(),
                    "content_type_id", contentType.id(),
                    "content_type_variable", contentType.variable(),
                    "content_type_idate", String.valueOf(contentType.iDate().getTime()),
                    "content_type_mod_date", String.valueOf(contentType.modDate().getTime()),
                    "host", host.getIdentifier(),
                    "workflows_ids", workflowSchemes.stream().map(WorkflowScheme::getId).collect(Collectors.toList()),
                    "workflows_names", workflowSchemes.stream().map(WorkflowScheme::getName).collect(Collectors.toList()),
                    "folder_id", contentType.folder()
            );

            if (!contentType.fields().isEmpty()) {
                final Field field = contentType.fields().get(0);
                addFieldArguments(arguments, field, "");

                final List<Field> categoryFields = contentType.fields().stream().filter(field1 -> ImmutableCategoryField.class.isInstance(field1))
                        .collect(Collectors.toList());

                final List<Field> relationshipFields = contentType.fields().stream().filter(field1 -> ImmutableRelationshipField.class.isInstance(field1))
                        .collect(Collectors.toList());

                if (!categoryFields.isEmpty()) {
                    addFieldArguments(arguments, categoryFields.get(0), "category_");
                }

                if (!relationshipFields.isEmpty()) {
                    addFieldArguments(arguments, relationshipFields.get(0), "relationship_");
                }
            }

            return arguments;
        } catch (DotDataException | DotSecurityException e) {
            throw  new RuntimeException(e);
        }
    }

    private static void addFieldArguments(final Map<String, Object> arguments, final Field field, final String prefix) {
        arguments.put(prefix + "field_id", field.id());
        arguments.put(prefix + "field_moddate", String.valueOf(field.modDate().getTime()));
        arguments.put(prefix + "field_name", field.name());
        arguments.put(prefix + "field_variable", field.variable());
        arguments.put(prefix + "field_values", field.values());
        arguments.put(prefix + "field_hint", field.hint());
        arguments.put(prefix + "field_default_value", field.defaultValue());
        arguments.put(prefix + "field_idate", String.valueOf(field.iDate().getTime()));
        arguments.put(prefix + "field_relation_type", field.relationType());
    }

    @Override
    public String getFilePathExpected(File file) {
        return "/bundlers-test/content_types/content_types_without_fields.contentType.json";
    }

    @Override
    public File getFileInner(final ContentType contentType, final File bundleRoot) {

        try {
            final Host host = APILocator.getHostAPI().find(contentType.host(), APILocator.systemUser(), false);
            return FileBundlerTestUtil.getContentTypeFilePath(contentType, host, bundleRoot);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean checkFileContent(ContentType contentType) {
        return !contentType.name().equals("Host") && !contentType.name().equals("File Asset") &&
                !ImmutableKeyValueContentType.class.isInstance(contentType);
    }
}
