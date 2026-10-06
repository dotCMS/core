package com.dotcms.graphql.datafetcher;

import static com.dotcms.contenttype.model.type.BaseContentType.DOTASSET;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_DESCRIPTION_FIELD_VAR;

import com.dotcms.graphql.InterfaceType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;

/**
 * Resolves {@code description} on asset content, answering differently depending on whether the
 * asset was reached through an Image or File field or queried directly.
 *
 * <p><b>This looks wrong and is not.</b> Two different meanings already share this name in
 * production, and they always have:
 *
 * <pre>
 *   image { description }            -> the contentlet TITLE (for a DOTASSET, the file name)
 *   ImagesCollection { description } -> the stored description, usually empty
 * </pre>
 *
 * <p>Same asset, same field name, different values — verified on a running instance, where the
 * first returned a value for 57 of 57 images while only 2 of those 57 have a stored description.
 * The flat view an asset-pointing field used to resolve to computed its own {@code description}
 * from the title; the content type's field holds what an editor typed.
 *
 * <p>Once an asset-pointing field is described by an interface, the concrete type's field
 * definition is what resolves — so without this, the first query above would silently start
 * answering with the second's value. That is the one outcome issue #34540 refuses: a name that
 * keeps working while returning something else. Declaring a single meaning would have forced a
 * choice between breaking one set of clients or the other.
 *
 * <p>So the meaning is selected by where the query came from, which preserves both contracts
 * exactly. This does not introduce path-dependence — it conserves the path-dependence that already
 * shipped.
 *
 * @see com.dotcms.graphql.InterfaceType#ASSET_INTERFACE_NAME
 */
public class AssetDescriptionDataFetcher implements DataFetcher<String> {

    @Override
    public String get(final DataFetchingEnvironment environment) {
        final Contentlet contentlet = environment.getSource();
        if (null == contentlet) {
            return null;
        }

        if (reachedThroughAssetField(environment)) {
            // The flat view's behaviour, preserved verbatim: title for image-style content, the
            // stored value for file-style content, which is what it has always answered.
            return DOTASSET == contentlet.getContentType().baseType()
                    ? contentlet.getTitle()
                    : (String) contentlet.get(FILEASSET_DESCRIPTION_FIELD_VAR);
        }

        return (String) contentlet.get(FILEASSET_DESCRIPTION_FIELD_VAR);
    }

    /**
     * @return whether this field is being resolved on content reached through an Image or File
     * field, as opposed to queried directly through its own collection
     */
    private boolean reachedThroughAssetField(final DataFetchingEnvironment environment) {
        final ExecutionStepInfo parent = environment.getExecutionStepInfo().getParent();
        if (null == parent) {
            return false;
        }

        // Read the DECLARED type off the parent's field definition, not off the step info.
        // `parent.getUnwrappedNonNullType()` looks like the obvious call and silently gives the
        // wrong answer: for a field typed by an interface, graphql-java rewrites the step info to
        // the CONCRETE type it resolved before executing the sub-selection, so the interface is
        // already gone by the time this runs and every asset would look directly-queried. The
        // schema's field definition is not rewritten, so it still says what the query asked for.
        final GraphQLType declaredType =
                GraphQLTypeUtil.unwrapAll(parent.getFieldDefinition().getType());
        return declaredType instanceof GraphQLInterfaceType
                && InterfaceType.ASSET_INTERFACE_NAME
                        .equals(((GraphQLInterfaceType) declaredType).getName());
    }
}
