package com.dotcms.ai.api.embeddings.extractor;

import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.ai.api.embeddings.EmbeddingException;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class is the one in charge of getting the information from dotCMS and extract the actual text for the embedding knowledge base
 * @author jsanca
 */
@ApplicationScoped
public class DotCMSContentExtractor implements ContentExtractor {

    @Override // todo: see if variant may be eventually needed or not
    public ExtractedContent extract(final String identifier, final long languageId) throws Exception {

        Logger.debug(this, ()-> "Extracting content for identifier: " + identifier + ", languageId: " + languageId);

        final ExtractorContextInfo contextInfo = this.retrieveContextInfo();
        final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier(
                identifier, contextInfo.showLive, languageId, contextInfo.user, contextInfo.respectAnonPerms);

        if (null == contentlet) {

            throw new DoesNotExistException("Content not found, identifier: " + identifier + ", languageId: " + languageId);
        }

        final ContentType contentType = contentlet.getContentType();
        final String extractTextContent = extractContent(contentlet, contentType);

        return ExtractedContent.of(contentlet.getInode(), contentlet.getIdentifier(), contentlet.getLanguageId(),
                contentlet.getHost(), contentlet.getVariantId(), contentType.variable(), contentlet.getTitle(), extractTextContent);
    }

    private String extractContent(final Contentlet contentlet , final ContentType contentType) {

        // todo: by now it is a very simple abstraction, future forward will be a convention velocity script on the application/ai/extract/transformers/{{contentTypeVariable}}.totext.vtl

        final List<Field> fields = ContentToStringUtil.impl.get().guessWhatFieldsToIndex(contentlet);
        final Optional<String> extractTextOpt = ContentToStringUtil.impl.get().parseFields(contentlet, fields);

        if (!extractTextOpt.isPresent()) {
            throw new EmbeddingException("Couldn't extract the content for the contentlet: " + contentlet.getIdentifier() +
                    " content type: " + contentType.variable());
        }
        return extractTextOpt.get();
    }

    private ExtractorContextInfo retrieveContextInfo() {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (null != request) {

            final PageMode pageMode = PageMode.get(request);
            final User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
            return new ExtractorContextInfo(pageMode.showLive, null != user? user: APILocator.systemUser(), // todo we use system as a fallback but not nice
                    pageMode.respectAnonPerms);
        }

        final PageMode pageMode = PageMode.EDIT_MODE; // todo: not sure about this
        return new ExtractorContextInfo(pageMode.showLive, APILocator.systemUser(), // todo we use system as a fallback but not nice
                pageMode.respectAnonPerms);
    }

    @Override
    public List<ExtractedContent> list(final String host,
                                       final String contentTypeVarname,
                                       final Long languageId,
                                       final int limit,
                                       final int offset) throws Exception {

        Logger.debug(this, ()-> "Extracting a list by content type: " + contentTypeVarname);
        final ExtractorContextInfo contextInfo = this.retrieveContextInfo();
        final ContentType contentType = APILocator.getContentTypeAPI(contextInfo.user).find(contentTypeVarname);
        if (null == contentType) {

            throw new DoesNotExistException("Content Type not found, varname: " + contentTypeVarname);
        }

        final List<Contentlet>  contentlets =
                APILocator.getContentletAPI().findByStructure(contentType.inode(), contextInfo.user, contextInfo.respectAnonPerms, limit, offset);

        if (null == contentlets || contentlets.isEmpty()) {
            return List.of();
        }

        final List<ExtractedContent> extractedContents = new ArrayList<>();

        for (Contentlet contentlet : contentlets) {

            try {
                final String extractTextContent = extractContent(contentlet, contentType);

                extractedContents.add(ExtractedContent.of(contentlet.getInode(), contentlet.getIdentifier(), contentlet.getLanguageId(),
                        contentlet.getHost(), contentlet.getVariantId(), contentType.variable(), contentlet.getTitle(), extractTextContent));
            } catch (Exception e) {
                Logger.debug(this, ()-> "Error extracting content from contentlet: " + contentlet.getIdentifier() +
                        ", content type: " + contentTypeVarname + ", msg: " + e.getMessage());
            }
        }

        return extractedContents;
    }

    private class ExtractorContextInfo {
        private final boolean showLive;
        private final User user;
        private final boolean respectAnonPerms;

        public ExtractorContextInfo(final boolean showLive, final User user, final boolean respectAnonPerms) {
            this.showLive = showLive;
            this.user = user;
            this.respectAnonPerms = respectAnonPerms;
        }
    }
}
