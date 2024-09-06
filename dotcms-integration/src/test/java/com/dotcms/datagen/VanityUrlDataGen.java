package com.dotcms.datagen;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;

import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class used to create {@link Contentlet} objects of type {@link com.dotcms.vanityurl.model.VanityUrl} for test purposes
 *
 * @author Nollymar Longa
 */
public class VanityUrlDataGen extends ContentletDataGen {
  private String uri;
  private String forwardTo;
  private int action;
  private int order;
  private String title;

  private static final AtomicReference<ContentType> vanityUrlContentType = new AtomicReference<>();

  private static Object lock = new Object();

  public VanityUrlDataGen() {
    super(createOrGetVanityURLContentType());
    structure(Objects.requireNonNull(vanityUrlContentType.get()).id());
    this.language(Try.of(()->APILocator.getLanguageAPI().getDefaultLanguage().getId()).getOrElseThrow(
            DotRuntimeException::new));
    this.host(APILocator.systemHost());
  }

  private static synchronized  ContentType createOrGetVanityURLContentType() {
    ContentType vanityType = vanityUrlContentType.get();
    if (vanityType!=null) {
        // Check if the content type still exists
        try {
          vanityType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(vanityType.id());
        } catch (Exception e) {
          vanityType = null;
        }
    }

    vanityType =  new ContentTypeDataGen()
           .baseContentType(BaseContentType.VANITY_URL)
           .nextPersisted();

    vanityUrlContentType.set(vanityType);
    return vanityType;

  }

  public VanityUrlDataGen uri(final String uri) {
    this.uri = uri;
    return this;
  }

  
  public VanityUrlDataGen forwardTo(final String forwardTo) {
    this.forwardTo = forwardTo;
    return this;
  }

  public VanityUrlDataGen site(final String site) {
    this.host(Try.of(()->APILocator.getHostAPI().find(site, APILocator.systemUser(), false)).getOrElseThrow(e->new DotRuntimeException(e)));
    return this;
  }
  
  public VanityUrlDataGen title(final String title) {
    this.title = title;
    return this;
  }

  public VanityUrlDataGen action(final int action) {
    this.action = action;
    return this;
  }

  public VanityUrlDataGen order(final int order) {
    this.order = order;
    return this;
  }
  public VanityUrlDataGen folder(final Folder folder) {
    this.folder = folder;
    return this;
  }
  public VanityUrlDataGen language(final long language) {
    this.languageId = language;
    return this;
  }
  @Override
  public DefaultVanityUrl next() {
    DefaultVanityUrl url = new DefaultVanityUrl();
    url.setAction(action);
    url.setContentTypeId(this.contentTypeId);
    url.setOrder(order);
    url.setURI(uri);
    url.setLanguageId(languageId);
    url.setTitle(UtilMethods.isSet(title) ? title : "Vanity Test " + System.currentTimeMillis());
    url.setForwardTo(forwardTo);
    url.setHost(this.host.getIdentifier());

    return url;
  }

  @Override
  public Contentlet nextPersisted() {
    DefaultVanityUrl url = next();

    url.setIndexPolicy(IndexPolicy.FORCE);
    url.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
    url.setBoolProperty(Contentlet.IS_TEST_MODE, true);
    Contentlet contentlet = persist(url);
    try {
      return (Contentlet) APILocator.getVanityUrlAPI().fromContentlet(contentlet);
    } catch (Exception e) {
      throw new DotRuntimeException(e);

    }
  }


  public VanityUrlDataGen allSites() {
    host = APILocator.systemHost();
    return this;
  }
}
