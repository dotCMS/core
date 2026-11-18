package com.dotcms.rendering.velocity.viewtools;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

/**
 * Exposes File Assets to Velocity as {@code $filetool}. Contents are looked up with the
 * permissions of the user making the request (the anonymous user when nobody is logged in), so a
 * file the current user cannot read is returned as {@code null}.
 */
public class FileTool implements ViewTool {

	private static final LanguageAPI languageAPI = APILocator.getLanguageAPI();
	private static final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

	private HttpServletRequest request;

	/**
	 * Keeps the request the tool was created for, used when no request is bound to the current
	 * thread.
	 *
	 * @param initData the {@link ViewContext} of the rendering, or any other object to ignore
	 */
	public void init(Object initData) {
		if (initData instanceof ViewContext) {
			this.request = ((ViewContext) initData).getRequest();
		}
	}

	/**
	 * @return the user making the current request, or the anonymous user if there is none. The
	 * request bound to the current thread wins over the one the tool was created for, so a tool
	 * instance shared between requests still answers for the current one.
	 */
	protected User getRequestUser() {
		final HttpServletRequest threadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final HttpServletRequest currentRequest = null != threadRequest ? threadRequest : this.request;
		User user = null;
		if (null != currentRequest) {
			user = WebAPILocator.getUserWebAPI().getUser(currentRequest);
		}
		return null != user ? user : APILocator.getUserAPI().getAnonymousUserNoThrow();
	}

	private Contentlet findAsRequestUser(final String inode) throws DotDataException {
		final User user = getRequestUser();
		try {
			return APILocator.getContentletAPI().find(inode, user, true);
		} catch (final DotSecurityException e) {
			Logger.debug(FileTool.class.getName(), () -> String.format(
					"User '%s' cannot read content with inode '%s'", user.getUserId(), inode));
			return null;
		}
	}

	public Contentlet getFile(String identifier, boolean live) throws DotStateException, DotDataException, DotSecurityException{
		return getFile(identifier, live, languageAPI.getDefaultLanguage().getId());
	}

	public Contentlet getFile(String identifier, boolean live, long languageId) throws DotDataException, DotStateException, DotSecurityException{
		final String conInode = getContentInode(identifier, live, languageId);
		final Contentlet contentlet = findAsRequestUser(conInode);
		if(contentlet==null) {
		    return null;
		}
		if(contentlet.isFileAsset()) {
		    return  APILocator.getFileAssetAPI().fromContentlet(contentlet);
		}

	    return contentlet;
	}

	private String  getContentInode(String identifier, boolean live, long languageId)
			throws DotDataException {
		Identifier id = identifierAPI.find(identifier);

		final String currentVariantName = WebAPILocator.getVariantWebAPI().currentVariantId();
		Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(),
				languageId, currentVariantName);

		if(cvi.isEmpty()) {
			cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(),
					languageId, VariantAPI.DEFAULT_VARIANT.name());
		}

		if(cvi.isEmpty()) {
			throw new DotDataException("Can't find Content-version-info. Identifier: " + id.getId() + ". Lang:" + languageId);
		}
		String conInode = !live ? cvi.get().getWorkingInode() : cvi.get().getLiveInode();
		return conInode;
	}

	public Contentlet getFileAsContentlet(String identifier, boolean live, long languageId) throws DotDataException, DotStateException, DotSecurityException{
		try {
		    return getFile(identifier, live, languageId);
		} catch(DotStateException e) {
			final String conInode = getContentInode(identifier, live, languageId);
			return findAsRequestUser(conInode);
		}
	}

	public String getURI(FileAsset file){
		return getURI(file, -1);
	}

	public String getURI(FileAsset file, long languageId){
		String langStr = languageId>0?"?language_id="+languageId:"";

		if(file != null && InodeUtils.isSet(file.getIdentifier())){
            return UtilMethods.espaceForVelocity("/contentAsset/raw-data/" + file.getIdentifier() + "/fileAsset" + langStr);
        }else{
			return "";
		}
	}

	public String getURI(final Contentlet contentlet, long languageId){
		String uri = StringPool.BLANK;

		if (contentlet instanceof FileAsset) {
			uri = getURI((FileAsset) contentlet, languageId);
		} else if (contentlet.isDotAsset()){
			uri = UtilMethods.espaceForVelocity(
					String.format("dA/%s", contentlet.getIdentifier())
			);
		}
		return uri;
	}
	
	public IFileAsset getNewFile(){
	    return new FileAsset(); 
	}

}
