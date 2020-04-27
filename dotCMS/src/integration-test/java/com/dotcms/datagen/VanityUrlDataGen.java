package com.dotcms.datagen;

import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;

import io.vavr.control.Try;

/**
 * Class used to create {@link Contentlet} objects of type Persona for test purposes
 *
 * @author Nollymar Longa
 */
public class VanityUrlDataGen extends ContentletDataGen {



  private String uri;
  private String forwardTo;
  private int action;
  private int order;
  private long language;
  private String title;
  private Folder folder;
  
  public VanityUrlDataGen() {
    super(Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser()).find(VanityUrlAPI.DEFAULT_VANITY_URL_STRUCTURE_VARNAME).id()).getOrElseThrow(e->new DotRuntimeException(e)));
    this.language(Try.of(()->APILocator.getLanguageAPI().getDefaultLanguage().getId()).getOrElseThrow(e->new DotRuntimeException(e)));
    this.host(APILocator.systemHost());
    this.folder(Try.of(()->APILocator.getFolderAPI().findSystemFolder()).getOrElseThrow(e->new DotRuntimeException(e)));
  }

  public VanityUrlDataGen uri(String uri) {
    this.uri = uri;
    return this;
  }

  
  public VanityUrlDataGen forwardTo(String forwardTo) {
    this.forwardTo = forwardTo;
    return this;
  }

  public VanityUrlDataGen site(final String site) {
    this.host(Try.of(()->APILocator.getHostAPI().find(site, APILocator.systemUser(), false)).getOrElseThrow(e->new DotRuntimeException(e)));
    return this;
  }
  
  public VanityUrlDataGen title(String title) {
    this.title = title;
    return this;
  }

  public VanityUrlDataGen action(int action) {
    this.action = action;
    return this;
  }

  public VanityUrlDataGen order(int order) {
    this.order = order;
    return this;
  }
  public VanityUrlDataGen folder(Folder folder) {
    this.folder = folder;
    return this;
  }
  public VanityUrlDataGen language(long language) {
    this.language = language;
    return this;
  }
  @Override
  public DefaultVanityUrl next() {
    DefaultVanityUrl url = new DefaultVanityUrl();
    url.setAction(action);
    url.setContentTypeId(this.contentTypeId);
    url.setOrder(order);
    url.setURI(uri);
    url.setLanguageId(language);
    url.setTitle(title);
    url.setForwardTo(forwardTo);
    url.setFolder(folder.getIdentifier());
    url.setSite(this.host.getIdentifier());
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

  
}
