package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.content.elasticsearch.business.SearchCriteria;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Interceptor;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.hook.HTMLPageHook;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedContentList;
import com.dotmarketing.util.contentet.pagination.PaginatedContentlets;
import com.liferay.portal.model.User;
import org.elasticsearch.action.search.SearchResponse;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This interceptor class allows developers to execute Java <b>code</b> before
 * and/or <b>after</b> calling the original {@link ContentletAPI} methods in
 * dotCMS. This way, the default process can be customizable and your own
 * routines can be triggered when calling a method in the API. For example,
 * contentlet information can be changed before being saved depending on
 * specific criteria, or an e-mail can be sent after a content with a specific
 * content type is saved.
 * 
 * @author Jason Tesser
 * @since 1.6.5c
 *
 */
public class ContentletAPIInterceptor implements ContentletAPI, Interceptor {

	private List<ContentletAPIPreHook> preHooks = new ArrayList<>();
	private List<ContentletAPIPostHook> postHooks = new ArrayList<>();
	private final ContentletAPI conAPI;

	private static final String PREHOOK_FAILED_MESSAGE = "The following prehook failed: %s";

	/**
	 * Default class constructor.
	 */
	public ContentletAPIInterceptor() {
		conAPI = APILocator.getContentletAPIImpl();
		try {
            this.addPostHook(new HTMLPageHook());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            Logger.warn(this.getClass(), e.getMessage(), e);
        }
	}

	@Override
	public void addLinkToContentlet(Contentlet contentlet, String linkInode,String relationName, User user, boolean respectFrontendRoles)	throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.addLinkToContentlet(contentlet, linkInode, relationName, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.addLinkToContentlet(contentlet, linkInode, relationName, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.addLinkToContentlet(contentlet, linkInode, relationName, user, respectFrontendRoles);
		}
	}

	@Override
	public void archive(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.archive(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.archive(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.archive(contentlet, user, respectFrontendRoles);
		}
	}

	@Override
	public void archive(List<Contentlet> contentlets, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.archive(contentlets, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.archive(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.archive(contentlets, user, respectFrontendRoles);
		}
	}

	@Override
	public Contentlet checkin(Contentlet contentlet,Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats, List<Permission> permissions, User user,	boolean respectFrontendRoles) throws IllegalArgumentException,		DotDataException, DotSecurityException,		DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles, c);
		}
		return c;
	}

	@Override
	public Contentlet checkin(Contentlet currentContentlet,	ContentletRelationships relationshipsData, List<Category> cats,	List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles) throws IllegalArgumentException,	DotDataException, DotSecurityException,	DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkin(currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles, c);
		}
		return c;
	}

	@Override
	public Contentlet checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats,
							  List<Permission> selectedPermissions, User user, boolean respectFrontendRoles, boolean generateSystemEvent) throws IllegalArgumentException, DotDataException, DotSecurityException, DotContentletStateException, DotContentletValidationException {
		for ( ContentletAPIPreHook pre : preHooks ) {
			boolean preResult = pre.checkin(currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles);
			if ( !preResult ) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkin(currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles, generateSystemEvent);
		for ( ContentletAPIPostHook post : postHooks ) {
			post.checkin(currentContentlet, relationshipsData, cats, selectedPermissions, user, respectFrontendRoles, c);
		}
		return c;
	}

	@Override
	public Contentlet checkin(final Contentlet contentlet,
							  final ContentletDependencies contentletDependencies) throws DotSecurityException, DotDataException {

		for ( ContentletAPIPreHook pre : preHooks ) {
			boolean preResult = pre.checkin(contentlet, contentletDependencies);
			if ( !preResult ) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		Contentlet c = conAPI.checkin(contentlet, contentletDependencies);
		for ( ContentletAPIPostHook post : postHooks ) {
			post.checkin(contentlet, contentletDependencies, c);
		}

		return c;
	}

	@Override
	public Contentlet checkin(Contentlet contentlet, List<Category> cats,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,	DotDataException, DotSecurityException,	DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, cats, permissions, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkin(contentlet, cats, permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, cats, permissions, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public Contentlet checkin(Contentlet contentlet,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,	DotDataException, DotSecurityException,DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, permissions, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkin(contentlet, permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, permissions, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public Contentlet checkin(Contentlet contentlet, User user,boolean respectFrontendRoles, List<Category> cats) throws IllegalArgumentException, DotDataException,DotSecurityException, DotContentletStateException,	DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, user, respectFrontendRoles, cats);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkin(contentlet, user, respectFrontendRoles, cats);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, user, respectFrontendRoles, cats, c);
		}
		return c;
	}

	@Override
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,List<Category> cats, User user, boolean respectFrontendRoles)throws IllegalArgumentException, DotDataException,	DotSecurityException, DotContentletStateException,DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public Contentlet checkin(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws IllegalArgumentException, DotDataException, DotSecurityException,	DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkin(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public Contentlet checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, User user, boolean respectFrontendRoles)	throws IllegalArgumentException, DotDataException,	DotSecurityException, DotContentletStateException,	DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkin(contentlet, contentRelationships, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkin(contentlet, contentRelationships, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkin(contentlet, contentRelationships, user, respectFrontendRoles,c);
		}
		return c;
	}

    /**
     * @deprecated Use {@link ContentletAPIInterceptor#checkinWithoutVersioning(Contentlet, ContentletRelationships, List, List, User, boolean)}
     */
	@Override
	public Contentlet checkinWithoutVersioning(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,	List<Category> cats, List<Permission> permissions, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkinWithoutVersioning(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkinWithoutVersioning(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkinWithoutVersioning(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles, c);
		}
		return c;
	}

    @Override
    public Contentlet checkinWithoutVersioning(Contentlet contentlet, ContentletRelationships contentRelationships,	List<Category> cats, List<Permission> permissions, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException, DotContentletValidationException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.checkinWithoutVersioning(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
            if(!preResult){
                String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        Contentlet c = conAPI.checkinWithoutVersioning(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles);
        for(ContentletAPIPostHook post : postHooks){
            post.checkinWithoutVersioning(contentlet, contentRelationships, cats, permissions, user, respectFrontendRoles, c);
        }
        return c;
    }

	@Override
	public Contentlet checkout(String contentletInode, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkout(contentletInode, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.checkout(contentletInode, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkout(contentletInode, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> checkout(List<Contentlet> contentlets, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkout(contentlets, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.checkout(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkout(contentlets, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> checkoutWithQuery(String luceneQuery, User user,	boolean respectFrontendRoles) throws DotDataException,DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkoutWithQuery(luceneQuery, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.checkoutWithQuery(luceneQuery, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.checkout(luceneQuery, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> checkout(String luceneQuery, User user, boolean respectFrontendRoles, int offset, int limit) throws DotDataException, DotSecurityException,	DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.checkout(luceneQuery, user, respectFrontendRoles, offset, limit);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.checkout(luceneQuery, user, respectFrontendRoles, offset, limit);
		for(ContentletAPIPostHook post : postHooks){
			post.checkout(luceneQuery, user, respectFrontendRoles, offset, limit,c);
		}
		return c;
	}

	@Override
	public void cleanField(Structure structure, Field field, User user,	boolean respectFrontendRoles) throws DotSecurityException,	DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.cleanField(structure, field, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.cleanField(structure, field, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.cleanField(structure, field, user, respectFrontendRoles);
		}
	}

    @Override
    public void cleanField(Structure structure, Date deletionDate, Field field, User user,
            boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.cleanField(structure, deletionDate, field, user, respectFrontendRoles);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        conAPI.cleanField(structure, deletionDate, field, user, respectFrontendRoles);
        for(ContentletAPIPostHook post : postHooks){
            post.cleanField(structure, deletionDate, field, user, respectFrontendRoles);
        }

    }

	@Override
	public void copyProperties(Contentlet contentlet, Map<String, Object> properties) throws DotContentletStateException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyProperties(contentlet, properties);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.copyProperties(contentlet, properties);
		for(ContentletAPIPostHook post : postHooks){
			post.copyProperties(contentlet, properties);
		}
	}

	@Override
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.delete(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean noErrors = conAPI.delete(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.delete(contentlet, user, respectFrontendRoles);
		}

		return noErrors;
	}

	@Override
	public boolean delete(Contentlet contentlet, User user, boolean respectFrontendRoles, boolean allVersions)	throws DotDataException, DotSecurityException,DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.delete(contentlet, user, respectFrontendRoles, allVersions);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean delete = conAPI.delete(contentlet, user, respectFrontendRoles, allVersions);
		for(ContentletAPIPostHook post : postHooks){
			post.delete(contentlet, user, respectFrontendRoles, allVersions);
		}

		return delete;
	}

	@Override
	public boolean destroy(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.destroy(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean noErrors = conAPI.destroy(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.destroy(contentlet, user, respectFrontendRoles);
		}

		return noErrors;
	}

	@Override
	public boolean destroy(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.destroy(contentlets, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean noErrors = conAPI.destroy(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.destroy(contentlets, user, respectFrontendRoles);
		}

		return noErrors;
	}

	@Override
    public boolean deleteByHost(Host host, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        for (ContentletAPIPreHook pre : preHooks) {
            boolean preResult = pre.deleteByHost(host, user, respectFrontendRoles);
            if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        boolean noErrors = conAPI.deleteByHost(host, user, respectFrontendRoles);
        for (ContentletAPIPostHook post : postHooks) {
            post.deleteByHost(host, user, respectFrontendRoles);
        }

        return noErrors;
    }

    @Override
    public boolean delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.delete(contentlets, user, respectFrontendRoles);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        boolean noErrors = conAPI.delete(contentlets, user, respectFrontendRoles);
        for(ContentletAPIPostHook post : postHooks){
            post.delete(contentlets, user, respectFrontendRoles);
        }

        return noErrors;
    }

    @Override
	public void delete(List<Contentlet> contentlets, User user,	boolean respectFrontendRoles, boolean allVersions) throws DotDataException, DotSecurityException,	DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.delete(contentlets, user, respectFrontendRoles, allVersions);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.delete(contentlets, user, respectFrontendRoles, allVersions);
		for(ContentletAPIPostHook post : postHooks){
			post.delete(contentlets, user, respectFrontendRoles, allVersions);
		}
	}

	@Override
	public int deleteOldContent(Date deleteFrom) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.deleteOldContent(deleteFrom);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		int c = conAPI.deleteOldContent(deleteFrom);
		for(ContentletAPIPostHook post : postHooks){
			post.deleteOldContent(deleteFrom,c);
		}
		return c;
	}

	@Override
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.deleteRelatedContent(contentlet, relationship, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.deleteRelatedContent(contentlet, relationship, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.deleteRelatedContent(contentlet, relationship, user, respectFrontendRoles);
		}
	}

	@Override
	public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, boolean hasParent, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.deleteRelatedContent(contentlet, relationship, hasParent, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.deleteRelatedContent(contentlet, relationship, hasParent, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.deleteRelatedContent(contentlet, relationship, hasParent, user, respectFrontendRoles);
		}
	}

    @Override
    public void deleteRelatedContent(final Contentlet contentlet, final Relationship relationship,
            final boolean hasParent, final User user, final boolean respectFrontendRoles,
            final List<Contentlet> contentletsToBeRelated)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        for (ContentletAPIPreHook pre : preHooks) {
            boolean preResult = pre.deleteRelatedContent(contentlet, relationship, hasParent, user,
                    respectFrontendRoles, contentletsToBeRelated);
            if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        conAPI.deleteRelatedContent(contentlet, relationship, hasParent, user, respectFrontendRoles,
                contentletsToBeRelated);
        for (ContentletAPIPostHook post : postHooks) {
            post.deleteRelatedContent(contentlet, relationship, hasParent, user,
                    respectFrontendRoles, contentletsToBeRelated);
        }
    }

    @Override
    public void invalidateRelatedContentCache(Contentlet contentlet, Relationship relationship,
            boolean hasParent) {
        for (ContentletAPIPreHook pre : preHooks) {
            boolean preResult = pre.invalidateRelatedContentCache(contentlet, relationship, hasParent);
            if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        conAPI.invalidateRelatedContentCache(contentlet, relationship, hasParent);
        for (ContentletAPIPostHook post : postHooks) {
            post.invalidateRelatedContentCache(contentlet, relationship, hasParent);
        }
    }

	@Override
	public Contentlet move(final Contentlet contentlet, final User user, final String hostAndFolderPath, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			final boolean preResult = pre.move(contentlet, user, hostAndFolderPath, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		final Contentlet resultContentlet = conAPI.move(contentlet, user, hostAndFolderPath, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.move(contentlet, user, hostAndFolderPath, respectFrontendRoles);
		}
		return resultContentlet;
	}

	@Override
	public Contentlet move(final Contentlet contentlet, final User user, final Host host, final String folderFolderPath, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			final boolean preResult = pre.move(contentlet, user, host, folderFolderPath, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		final Contentlet c = conAPI.move(contentlet, user, host, folderFolderPath, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.move(contentlet, user, host, folderFolderPath, respectFrontendRoles);
		}
		return c;
	}

	@Override
	public Contentlet move(final Contentlet contentlet, User user, Host host, Folder folder, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			final boolean preResult = pre.move(contentlet, user, host, folder, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		final Contentlet c = conAPI.move(contentlet, user, host, folder, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.move(contentlet, user, host, folder, respectFrontendRoles);
		}
		return c;
	}

	@Override
	public Contentlet find(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return find(inode, user, respectFrontendRoles, false);
	}

	@Override
	public Contentlet find(String inode, User user, boolean respectFrontendRoles, boolean ignoreStoryBlock) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.find(inode, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.find(inode, user, respectFrontendRoles, ignoreStoryBlock);
		for(ContentletAPIPostHook post : postHooks){
			post.find(inode, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> find(Category category, long languageId, boolean live, String orderBy, User user, boolean respectFrontendRoles) throws DotDataException,	DotContentletStateException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.find(category, languageId, live, orderBy, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.find(category, languageId, live, orderBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.find(category, languageId, live, orderBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> find(List<Category> categories, long languageId, boolean live, String orderBy, User user,boolean respectFrontendRoles) throws DotDataException,	DotContentletStateException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.find(categories, languageId, live, orderBy, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.find(categories, languageId, live, orderBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.find(categories, languageId, live, orderBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> findAllContent(int offset, int limit) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findAllContent(offset, limit);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findAllContent(offset, limit);
		for(ContentletAPIPostHook post : postHooks){
			post.findAllContent(offset, limit,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> findAllUserVersions(Identifier identifier,User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findAllUserVersions(identifier, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findAllUserVersions(identifier, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findAllUserVersions(identifier, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles) throws DotSecurityException,	DotDataException, DotStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findAllVersions(identifier, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findAllVersions(identifier, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findAllVersions(identifier, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> findLiveOrWorkingVersions(Set<String> identifiers, User user,
			boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findLiveOrWorkingVersions(identifiers, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findLiveOrWorkingVersions(identifiers, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findLiveOrWorkingVersions(identifiers, user, respectFrontendRoles);
		}
		return c;
	}

	@Override
	public List<Contentlet> findAllVersions(final Identifier identifier, final Variant variant,
			final User user, final boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findAllVersions(identifier, variant, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findAllVersions(identifier, variant, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findAllVersions(identifier, variant, user, respectFrontendRoles);
		}
		return c;
	}

	@Override
	public List<Contentlet> findAllVersions(Identifier identifier, boolean bringOldVersions, User user, boolean respectFrontendRoles) throws DotSecurityException,	DotDataException, DotStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findAllVersions(identifier, bringOldVersions, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findAllVersions(identifier, bringOldVersions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findAllVersions(identifier, bringOldVersions, user, respectFrontendRoles,c);
		}
		return c;
	}

    @Override
    public List<Contentlet> findAllVersions(final SearchCriteria searchCriteria) throws DotSecurityException, DotDataException, DotStateException {
        for (final ContentletAPIPreHook preHook : preHooks) {
            final boolean preResult = preHook.findAllVersions(searchCriteria);
            if (!preResult) {
                final String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, preHook.getClass().getName());
                Logger.error(this, errorMessage);
                throw new DotRuntimeException(errorMessage);
            }
        }
        final List<Contentlet> contentlets = conAPI.findAllVersions(searchCriteria);
        for (final ContentletAPIPostHook postHook : postHooks) {
            postHook.findAllVersions(searchCriteria, contentlets);
        }
        return contentlets;
    }

	@Override
	public List<Contentlet> findByStructure(Structure structure, User user,	boolean respectFrontendRoles, int limit, int offset)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findByStructure(structure, user, respectFrontendRoles, limit, offset);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findByStructure(structure, user, respectFrontendRoles, limit, offset);
		for(ContentletAPIPostHook post : postHooks){
			post.findByStructure(structure, user, respectFrontendRoles, limit, offset,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> findByStructure(String structureInode, User user,	boolean respectFrontendRoles, int limit, int offset)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findByStructure(structureInode, user, respectFrontendRoles, limit, offset);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findByStructure(structureInode, user, respectFrontendRoles, limit, offset);
		for(ContentletAPIPostHook post : postHooks){
			post.findByStructure(structureInode, user, respectFrontendRoles, limit, offset,c);
		}
		return c;
	}

	@Override
	public Contentlet findContentletByIdentifier(String identifier, boolean live, long languageId, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException,	DotContentletStateException {
		return findContentletByIdentifier(identifier, live, languageId, VariantAPI.DEFAULT_VARIANT.name(), user, respectFrontendRoles);
	}

	@Override
	public Contentlet findContentletByIdentifier(String identifier, boolean live, long languageId, String variantId, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException,	DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletByIdentifier(identifier, live, languageId, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.findContentletByIdentifier(identifier, live, languageId, variantId, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletByIdentifier(identifier, live, languageId, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public Contentlet findContentletByIdentifier(String identifier, long languageId, String variantId,
			Date timeMachineDate, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException{
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletByIdentifier(identifier, languageId, variantId, user, timeMachineDate, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.findContentletByIdentifier(identifier, languageId, variantId,
				timeMachineDate, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletByIdentifier(identifier, languageId, variantId, user, timeMachineDate, respectFrontendRoles);
		}
		return c;
	}

    @Override
    public Contentlet findContentletByIdentifierAnyLanguage(String identifier, final boolean includeDeleted) throws DotDataException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.findContentletByIdentifierAnyLanguage(identifier, includeDeleted);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        Contentlet c = conAPI.findContentletByIdentifierAnyLanguage(identifier, includeDeleted);
        for(ContentletAPIPostHook post : postHooks){
            post.findContentletByIdentifierAnyLanguage(identifier, includeDeleted);
        }
        return c;
    }

	@Override
	public Contentlet findContentletByIdentifierAnyLanguage(String identifier) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletByIdentifierAnyLanguage(identifier);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.findContentletByIdentifierAnyLanguage(identifier);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletByIdentifierAnyLanguage(identifier);
		}
		return c;
	}

	@Override
	public Contentlet findContentletByIdentifierAnyLanguageAnyVariant(String identifier) throws DotDataException{
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletByIdentifierAnyLanguage(identifier);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.findContentletByIdentifierAnyLanguageAnyVariant(identifier);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletByIdentifierAnyLanguage(identifier);
		}
		return c;
	}

	@Override
	public Contentlet findContentletByIdentifierAnyLanguage(String identifier, String variant) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletByIdentifierAnyLanguage(identifier, variant);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.findContentletByIdentifierAnyLanguage(identifier, variant);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletByIdentifierAnyLanguage(identifier, variant);
		}
		return c;
	}

	@CloseDBIfOpened
	@Override
	public Contentlet findContentletByIdentifierAnyLanguage(final String identifier, final String variant,
			final boolean includeDeleted) throws DotDataException{
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletByIdentifierAnyLanguage(identifier, variant, includeDeleted);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.findContentletByIdentifierAnyLanguage(identifier, variant, includeDeleted);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletByIdentifierAnyLanguage(identifier, variant);
		}
		return c;
	}
	@Override
	public Contentlet findContentletForLanguage(long languageId, Identifier contentletId) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletForLanguage(languageId, contentletId);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.findContentletForLanguage(languageId, contentletId);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletForLanguage(languageId, contentletId,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> findContentlets(List<String> inodes) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentlets(inodes);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findContentlets(inodes);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentlets(inodes,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> findContentletsByFolder(Folder parentFolder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletsByFolder(parentFolder, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findContentletsByFolder(parentFolder, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletsByFolder(parentFolder, user, respectFrontendRoles);
		}
		return c;
	}

	@Override
	public List<Contentlet> findContentletsByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletsByHost(parentHost, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findContentletsByHost(parentHost, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletsByHost(parentHost, user, respectFrontendRoles);
		}
		return c;
	}

	@Override
    public List<Contentlet> findContentletsByHost(Host parentHost,
            List<Integer> includingContentTypes, List<Integer> excludingContentTypes, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        for (ContentletAPIPreHook pre : preHooks) {
            boolean preResult = pre.findContentletsByHost(parentHost, includingContentTypes,
                    excludingContentTypes, user, respectFrontendRoles);
            if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }

        List<Contentlet> c = conAPI.findContentletsByHost(parentHost, includingContentTypes,
                excludingContentTypes, user, respectFrontendRoles);

        for (ContentletAPIPostHook post : postHooks) {
            post.findContentletsByHost(parentHost, includingContentTypes,
                    excludingContentTypes, user, respectFrontendRoles);
        }
        return c;
    }



	public PaginatedContentlets findContentletsPaginatedByHost(Host parentHost, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return findContentletsPaginatedByHost(parentHost, null, null, user, respectFrontendRoles);
	}

	@Override
	public PaginatedContentlets findContentletsPaginatedByHost(
			final Host parentHost,
			final List<Integer> includingContentTypes,
			final List<Integer> excludingContentTypes,
			final User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException{
		for (ContentletAPIPreHook pre : preHooks) {
			boolean preResult = pre.findContentletsByHost(parentHost, includingContentTypes,
					excludingContentTypes, user, respectFrontendRoles);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		final PaginatedContentlets paginatedContentletsByHost = conAPI.findContentletsPaginatedByHost(
				parentHost, includingContentTypes,
				excludingContentTypes, user, respectFrontendRoles);

		for (ContentletAPIPostHook post : postHooks) {
			post.findContentletsByHost(parentHost, includingContentTypes,
					excludingContentTypes, user, respectFrontendRoles);
		}
		return paginatedContentletsByHost;
	}


	@Override
	public List<Contentlet> findContentletsByHostBaseType(Host parentHost,
												  List<Integer> includingBaseTypes, User user,
												  boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for (ContentletAPIPreHook pre : preHooks) {
			boolean preResult = pre.findContentletsByHostBaseType(parentHost, includingBaseTypes,
				user, respectFrontendRoles);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		List<Contentlet> c = conAPI.findContentletsByHostBaseType(parentHost, includingBaseTypes,
			user, respectFrontendRoles);

		for (ContentletAPIPostHook post : postHooks) {
			post.findContentletsByHostBaseType(parentHost, includingBaseTypes,
				user, respectFrontendRoles);
		}
		return c;
	}

	@Override
	public List<Contentlet> findContentletsByIdentifiers(String[] identifiers, boolean live, long languageId, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentletsByIdentifiers(identifiers, live, languageId, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findContentletsByIdentifiers(identifiers, live, languageId, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentletsByIdentifiers(identifiers, live, languageId, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<String> findFieldValues(String structureInode, Field field,User user, boolean respectFrontEndRoles) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findFieldValues(structureInode, field, user, respectFrontEndRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<String> c = conAPI.findFieldValues(structureInode, field, user, respectFrontEndRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findFieldValues(structureInode, field, user, respectFrontEndRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> findPageContentlets(String HTMLPageIdentifier,String containerIdentifier, String orderby, boolean working,	long languageId, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findPageContentlets(HTMLPageIdentifier, containerIdentifier, orderby, working, languageId, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.findPageContentlets(HTMLPageIdentifier, containerIdentifier, orderby, working, languageId, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.findPageContentlets(HTMLPageIdentifier, containerIdentifier, orderby, working, languageId, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getAllLanguages(contentlet, isLiveContent, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.getAllLanguages(contentlet, isLiveContent, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getAllLanguages(contentlet, isLiveContent, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public ContentletRelationships getAllRelationships(String contentletInode, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getAllRelationships(contentletInode, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		ContentletRelationships c = conAPI.getAllRelationships(contentletInode, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getAllRelationships(contentletInode, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public ContentletRelationships getAllRelationships(Contentlet contentlet) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getAllRelationships(contentlet);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		ContentletRelationships c = conAPI.getAllRelationships(contentlet);
		for(ContentletAPIPostHook post : postHooks){
			post.getAllRelationships(contentlet,c);
		}
		return c;
	}

	@Override
	public File getBinaryFile(String contentletInode,	String velocityVariableName, User user) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getBinaryFile(contentletInode, velocityVariableName, user);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		File c = conAPI.getBinaryFile(contentletInode, velocityVariableName, user);
		for(ContentletAPIPostHook post : postHooks){
			post.getBinaryFile(contentletInode, velocityVariableName, user,c);
		}
		return c;
	}

	@Override
	public List<Map<String, Object>> getContentletReferences(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException,	DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getContentletReferences(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Map<String, Object>> c = conAPI.getContentletReferences(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getContentletReferences(contentlet, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public Optional<Integer> getAllContentletReferencesCount(final String contentletId) throws DotDataException {
		for (final ContentletAPIPreHook pre : this.preHooks) {
			final boolean preResult = pre.getAllContentletReferencesCount(contentletId);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		final Optional<Integer> count = this.conAPI.getAllContentletReferencesCount(contentletId);
		for (final ContentletAPIPostHook post : this.postHooks) {
			post.getContentletReferenceCount(contentletId);
		}
		return count;
	}

	/**
	 * @deprecated use {@link ContentletAPIInterceptor#getFieldValue(Contentlet, com.dotcms.contenttype.model.field.Field)} instead
	 * @param contentlet
	 * @param theField a legacy field from the contentlet's parent Content Type
	 * @return
	 */
	@Deprecated
	@Override
	public Object getFieldValue(Contentlet contentlet, Field theField) {
		return getFieldValue(contentlet, new LegacyFieldTransformer(theField).from());
	}

	@Override
	public Object getFieldValue(Contentlet contentlet, com.dotcms.contenttype.model.field.Field theField) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getFieldValue(contentlet, theField);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Object c = conAPI.getFieldValue(contentlet, theField);
		for(ContentletAPIPostHook post : postHooks){
			post.getFieldValue(contentlet, theField,c);
		}
		return c;
	}

	@Override
	public Object getFieldValue(Contentlet contentlet, com.dotcms.contenttype.model.field.Field theField, User user, final boolean respectFrontEndRoles) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getFieldValue(contentlet, theField, user);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Object c = conAPI.getFieldValue(contentlet, theField, user,respectFrontEndRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getFieldValue(contentlet, theField,c, user);
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.contentlet.business.ContentletAPI#getName(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)
	 */

	public String getName(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotSecurityException, DotContentletStateException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getName(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		String c = conAPI.getName(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getName(contentlet, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> getRelatedContent(Contentlet contentlet,Relationship rel, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getRelatedContent(contentlet, rel, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.getRelatedContent(contentlet, rel, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getRelatedContent(contentlet, rel, user, respectFrontendRoles,c);
		}
		return c;
	}

    @Override
    public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel,
            Boolean pullByParent, User user, boolean respectFrontendRoles, int limit, int offset,
            String sortBy) throws DotDataException {
        for (ContentletAPIPreHook pre : preHooks) {
            boolean preResult = pre
                    .getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles,
                            limit, offset, sortBy);
            if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        List<Contentlet> c = conAPI
                .getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, limit,
                        offset, sortBy);
        for (ContentletAPIPostHook post : postHooks) {
            post.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, c, limit,
                    offset, sortBy);
        }
        return c;
    }

    @Override
    public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel,
            Boolean pullByParent, User user, boolean respectFrontendRoles, int limit, int offset,
            String sortBy, long language, Boolean live) throws DotDataException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, limit, offset, sortBy, language, live);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        List<Contentlet> c = conAPI.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, limit, offset, sortBy, language, live);
        for(ContentletAPIPostHook post : postHooks){
            post.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, limit, offset, sortBy, language, live);
        }
        return c;
    }

    @Override
    public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel,
            Boolean pullByParent, User user, boolean respectFrontendRoles, long language,
            Boolean live) throws DotDataException, DotSecurityException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, language, live);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        List<Contentlet> c = conAPI.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, language, live);
        for(ContentletAPIPostHook post : postHooks){
            post.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, language, live);
        }
        return c;
    }

    @Override
	public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel, Boolean pullByParent, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getRelatedContent(contentlet, rel,pullByParent, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles,c);
		}
		return c;
	}

    @Override
    public List<Contentlet> getRelatedContent(Contentlet contentlet, String variableName, User user,
            boolean respectFrontendRoles, Boolean pullByParents, int limit, int offset,
            String sortBy) {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.getRelatedContent(contentlet, variableName, user, respectFrontendRoles, pullByParents, limit, offset, sortBy);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        List<Contentlet> c = conAPI.getRelatedContent(contentlet, variableName, user, respectFrontendRoles, pullByParents, limit, offset, sortBy);
        for(ContentletAPIPostHook post : postHooks){
            post.getRelatedContent(contentlet, variableName, user, respectFrontendRoles, pullByParents, limit, offset, sortBy);
        }
        return c;
    }

    @Override
    public List<Contentlet> getRelatedContent(Contentlet contentlet, String variableName, User user,
            boolean respectFrontendRoles, Boolean pullByParents, int limit, int offset,
            String sortBy, long language, Boolean live) {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.getRelatedContent(contentlet, variableName, user, respectFrontendRoles, pullByParents, limit, offset, sortBy, language, live);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        List<Contentlet> c = conAPI.getRelatedContent(contentlet, variableName, user, respectFrontendRoles, pullByParents, limit, offset, sortBy, language, live);
        for(ContentletAPIPostHook post : postHooks){
            post.getRelatedContent(contentlet, variableName, user, respectFrontendRoles, pullByParents, limit, offset, sortBy, language, live);
        }
        return c;
    }

    @Override
	public Identifier getRelatedIdentifier(Contentlet contentlet, String relationshipType, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getRelatedIdentifier(contentlet, relationshipType, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Identifier c = conAPI.getRelatedIdentifier(contentlet, relationshipType, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getRelatedIdentifier(contentlet, relationshipType, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Link> getRelatedLinks(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getRelatedLinks(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Link> c = conAPI.getRelatedLinks(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getRelatedLinks(contentlet, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<ContentletSearch> searchIndex(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)	throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.searchIndex(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<ContentletSearch> c = conAPI.searchIndex(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.searchIndex(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public boolean isContentEqual(Contentlet contentlet1, Contentlet contentlet2, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isContentEqual(contentlet1, contentlet2, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isContentEqual(contentlet1, contentlet2, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.isContentEqual(contentlet1, contentlet2, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public boolean isContentlet(String inode) throws DotDataException, DotRuntimeException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isContentlet(inode);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isContentlet(inode);
		for(ContentletAPIPostHook post : postHooks){
			post.isContentlet(inode,c);
		}
		return c;
	}

	@Override
	public boolean isFieldTypeBoolean(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeBoolean(field);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isFieldTypeBoolean(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeBoolean(field,c);
		}
		return c;
	}

	@Override
	public boolean isFieldTypeDate(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeDate(field);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isFieldTypeDate(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeDate(field,c);
		}
		return c;
	}

	@Override
	public boolean isFieldTypeFloat(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeFloat(field);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isFieldTypeFloat(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeFloat(field,c);
		}
		return c;
	}

	@Override
	public boolean isFieldTypeLong(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeLong(field);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isFieldTypeLong(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeLong(field,c);
		}
		return c;
	}

	@Override
	public boolean isFieldTypeString(Field field) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isFieldTypeString(field);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isFieldTypeString(field);
		for(ContentletAPIPostHook post : postHooks){
			post.isFieldTypeString(field,c);
		}
		return c;
	}

	@Override
	public void lock(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.lock(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.lock(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.lock(contentlet, user, respectFrontendRoles);
		}
	}

	@Override
	public void publish(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotContentletStateException,	DotContentletStateException, DotStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.publish(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.publish(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.publish(contentlet, user, respectFrontendRoles);
		}
	}

	@Override
	public void publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.publish(contentlets, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.publish(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.publish(contentlets, user, respectFrontendRoles);
		}
	}

	@Override
	public void publishRelatedHtmlPages(Contentlet contentlet) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.publishRelatedHtmlPages(contentlet);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		try {
			conAPI.publishRelatedHtmlPages(contentlet);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
		for(ContentletAPIPostHook post : postHooks){
			post.publishRelatedHtmlPages(contentlet);
		}
	}

    @Override
	public void reindex() throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.reindex();
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.reindex();
		for(ContentletAPIPostHook post : postHooks){
			post.reindex();
		}
	}

	@Override
	public void reindex(Structure structure) throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.reindex(structure);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.reindex(structure);
		for(ContentletAPIPostHook post : postHooks){
			post.reindex(structure);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void reindex(Contentlet contentlet) throws DotReindexStateException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.reindex(contentlet);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.reindex(contentlet);
		for(ContentletAPIPostHook post : postHooks){
			post.reindex(contentlet);
		}
	}

	@Override
	public void refresh(Structure structure) throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refresh(structure);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.refresh(structure);
		for(ContentletAPIPostHook post : postHooks){
			post.refresh(structure);
		}
	}

    @Override
    public void refresh(ContentType type) throws DotReindexStateException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.refresh(type);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        conAPI.refresh(type);
        for(ContentletAPIPostHook post : postHooks){
            post.refresh(type);
        }
    }

	public Contentlet copyContentToVariant(final Contentlet contentlet, final String variantName,
			final User user) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.saveContentOnVariant(contentlet, variantName, user);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		final Contentlet saveContentOnVariant = conAPI.copyContentToVariant(contentlet, variantName, user);

		for(ContentletAPIPostHook post : postHooks){
			post.saveContentOnVariant(contentlet, variantName, user);
		}

		return saveContentOnVariant;
    }
	
	
	@Override
	public void refresh(Contentlet contentlet) throws DotReindexStateException,
			DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refresh(contentlet);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.refresh(contentlet);
		for(ContentletAPIPostHook post : postHooks){
			post.refresh(contentlet);
		}
	}

	@Override
	public void refreshAllContent() throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refreshAllContent();
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.refreshAllContent();
		for(ContentletAPIPostHook post : postHooks){
			post.refreshAllContent();
		}
	}

	@Override
	public void refreshContentUnderHost(Host host) throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refreshContentUnderHost(host);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.refreshContentUnderHost(host);
		for(ContentletAPIPostHook post : postHooks){
			post.refreshContentUnderHost(host);
		}
	}

	@Override
	public void relateContent(Contentlet contentlet, Relationship rel, List<Contentlet> related, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException,	DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.relateContent(contentlet, rel, related, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.relateContent(contentlet, rel, related, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.relateContent(contentlet, rel, related, user, respectFrontendRoles);
		}
	}

	@Override
	public void relateContent(Contentlet contentlet, ContentletRelationshipRecords related, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.relateContent(contentlet, related, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.relateContent(contentlet, related, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.relateContent(contentlet, related, user, respectFrontendRoles);
		}
	}

    /**
     * @deprecated This method should not be exposed. Use ContentletAPI.getRelated variations instead
     * @param contentlet
     * @param rel
     * @param user
     * @param respectFrontendRoles
     * @param pullByParent
     * @param limit
     * @param offset
     * @param sortBy
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Deprecated
    @Override
    public List<Contentlet> filterRelatedContent(Contentlet contentlet, Relationship rel, User user,
            boolean respectFrontendRoles, Boolean pullByParent, int limit, int offset,
            String sortBy) throws DotDataException, DotSecurityException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.filterRelatedContent(contentlet, rel, user, respectFrontendRoles, pullByParent,
                    limit, offset, sortBy);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        List<Contentlet> contentlets = conAPI
                .filterRelatedContent(contentlet, rel, user, respectFrontendRoles, pullByParent,
                        limit, offset, null);
        for(ContentletAPIPostHook post : postHooks){
            post.filterRelatedContent(contentlet, rel, user, respectFrontendRoles, pullByParent,
                    limit, offset, sortBy);
        }

        return contentlets;
    }

    @Override
	public void restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException,	DotContentletStateException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.restoreVersion(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.restoreVersion(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.restoreVersion(contentlet, user, respectFrontendRoles);
		}
	}

	@Override
	public PaginatedContentList<Contentlet> searchPaginatedByPage(final String luceneQuery,
			final int contentsPerPage, final int page, final String sortBy, final User user,
			final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		for (ContentletAPIPreHook pre : preHooks) {
			boolean preResult = pre.searchPaginatedByPage(
					luceneQuery, contentsPerPage, page, sortBy, user, respectFrontendRoles
			);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		var result = conAPI.searchPaginatedByPage(
				luceneQuery, contentsPerPage, page, sortBy, user, respectFrontendRoles
		);

		for (ContentletAPIPostHook post : postHooks) {
			post.searchPaginatedByPage(
					luceneQuery, contentsPerPage, page, sortBy, user, respectFrontendRoles
			);
		}

		return result;
	}

	@Override
	public PaginatedContentList<Contentlet> searchPaginated(final String luceneQuery,
			final int limit, final int offset, final String sortBy, final User user,
			final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		for (ContentletAPIPreHook pre : preHooks) {
			boolean preResult = pre.searchPaginated(
					luceneQuery, limit, offset, sortBy, user, respectFrontendRoles
			);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		var result = conAPI.searchPaginated(
				luceneQuery, limit, offset, sortBy, user, respectFrontendRoles
		);

		for (ContentletAPIPostHook post : postHooks) {
			post.searchPaginated(
					luceneQuery, limit, offset, sortBy, user, respectFrontendRoles
			);
		}

		return result;
	}

	@Override
	public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException,	DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission);
		for(ContentletAPIPostHook post : postHooks){
			post.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> getAllContentByVariants(User user,
			boolean respectFrontendRoles, String... variantNames)
			throws DotDataException, DotStateException, DotSecurityException {
		for (ContentletAPIPreHook pre : preHooks) {
			boolean preResult = pre.getAllContentByVariants(user, respectFrontendRoles, variantNames);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		List<Contentlet> c = conAPI.getAllContentByVariants(user, respectFrontendRoles, variantNames);

		for (ContentletAPIPostHook post : postHooks) {
			post.getAllContentByVariants(user, respectFrontendRoles, variantNames);
		}
		return c;
	}

	@Override
	public void addPermissionsToQuery ( StringBuffer buffy, User user, List<Role> roles, boolean respectFrontendRoles ) throws DotSecurityException, DotDataException {
		for ( ContentletAPIPreHook pre : preHooks ) {
			boolean preResult = pre.addPermissionsToQuery( buffy, user, roles, respectFrontendRoles );
			if ( !preResult ) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.addPermissionsToQuery( buffy, user, roles, respectFrontendRoles );
		for ( ContentletAPIPostHook post : postHooks ) {
			post.addPermissionsToQuery( buffy, user, roles, respectFrontendRoles );
		}
	}

	@Override
	public void setContentletProperty(Contentlet contentlet, Field field, Object value) throws DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.setContentletProperty(contentlet, field, value);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.setContentletProperty(contentlet, field, value);
		for(ContentletAPIPostHook post : postHooks){
			post.setContentletProperty(contentlet, field, value);
		}
	}

	@Override
	public void setContentletProperty(Contentlet contentlet, com.dotcms.contenttype.model.field.Field field, Object value) throws DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.setContentletProperty(contentlet, field, value);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.setContentletProperty(contentlet, field, value);
		for(ContentletAPIPostHook post : postHooks){
			post.setContentletProperty(contentlet, field, value);
		}
	}

	@Override
	public void unarchive(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unarchive(contentlets, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.unarchive(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unarchive(contentlets, user, respectFrontendRoles);
		}
	}

	@Override
	public void unarchive(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unarchive(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.unarchive(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unarchive(contentlet, user, respectFrontendRoles);
		}
	}

	@Override
	public void unlock(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException,	DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unlock(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.unlock(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unlock(contentlet, user, respectFrontendRoles);
		}
	}

	@Override
	public void unpublish(Contentlet contentlet, User user,	boolean respectFrontendRoles) throws DotDataException,	DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unpublish(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.unpublish(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unpublish(contentlet, user, respectFrontendRoles);
		}
	}

	@Override
	public void unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.unpublish(contentlets, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.unpublish(contentlets, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.unpublish(contentlets, user, respectFrontendRoles);
		}
	}

	@Override
	public void validateContentlet(Contentlet contentlet, List<Category> cats) throws DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.validateContentlet(contentlet, cats);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.validateContentlet(contentlet, cats);
		for(ContentletAPIPostHook post : postHooks){
			post.validateContentlet(contentlet, cats);
		}
	}

	@Override
	public void validateContentlet(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships,	List<Category> cats) throws DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.validateContentlet(contentlet, contentRelationships, cats);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.validateContentlet(contentlet, contentRelationships, cats);
		for(ContentletAPIPostHook post : postHooks){
			post.validateContentlet(contentlet, contentRelationships, cats);
		}
	}

	@Override
	public void validateContentlet(Contentlet contentlet, ContentletRelationships contentRelationships, List<Category> cats) throws DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.validateContentlet(contentlet, contentRelationships, cats);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.validateContentlet(contentlet, contentRelationships, cats);
		for(ContentletAPIPostHook post : postHooks){
			post.validateContentlet(contentlet, contentRelationships, cats);
		}
	}

	@Override
	public void validateContentletNoRels(Contentlet contentlet, List<Category> cats) throws DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.validateContentletNoRels(contentlet, cats);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.validateContentletNoRels(contentlet, cats);
		for(ContentletAPIPostHook post : postHooks){
			post.validateContentletNoRels(contentlet, cats);
		}
	}

	@Override
	public void validateContentlet(Contentlet contentlet,
			ContentletRelationships contentRelationships, List<Category> cats, boolean preview ) throws DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.validateContentletNoRels(contentlet, cats);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.validateContentlet(contentlet, contentRelationships, cats, preview);
		for(ContentletAPIPostHook post : postHooks){
			post.validateContentletNoRels(contentlet, cats);
		}
	}

	@Override
	public void validateContentletNoRels(Contentlet contentlet,
										   List<Category> cats, boolean preview) throws DotContentletValidationException {
			for(ContentletAPIPreHook pre : preHooks){
				boolean preResult = pre.validateContentletNoRels(contentlet, cats);
				if(!preResult){
					String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
					Logger.error(this, errorMessage);
					throw new DotRuntimeException(errorMessage);
				}
			}
			conAPI.validateContentletNoRels(contentlet, cats, preview);
			for(ContentletAPIPostHook post : postHooks){
				post.validateContentletNoRels(contentlet, cats);
			}
	}

	@Override
	public void addPreHook(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Object o = Class.forName(className).newInstance();
        addPreHook( o );
    }

	@Override
    public void addPreHook ( Object preHook ) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if ( preHook instanceof ContentletAPIPreHook ) {
            preHooks.add( (ContentletAPIPreHook) preHook );
		}else {
			throw new InstantiationException("This hook must implement ContentletAPIPrehook");
		}
	}

    @Override
	public void addPostHook(String className, int indexToAddAt) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		Object o = Class.forName(className).newInstance();
		if(o instanceof ContentletAPIPostHook){
			postHooks.add(indexToAddAt,(ContentletAPIPostHook)o);
		}else {
			throw new InstantiationException("This hook must implement ContentletAPIPosthook");
		}
	}

	@Override
	public void addPostHook(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Object o = Class.forName(className).newInstance();
        addPostHook( o );
    }

	@Override
    public void addPostHook ( Object postHook ) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if ( postHook instanceof ContentletAPIPostHook ) {
            postHooks.add( (ContentletAPIPostHook) postHook );
		}else {
			throw new InstantiationException("This hook must implement ContentletAPIPosthook");
		}
	}

    @Override
	public void addPreHook(String className, int indexToAddAt)	throws InstantiationException, IllegalAccessException,ClassNotFoundException {
		Object o = Class.forName(className).newInstance();
		if(o instanceof ContentletAPIPreHook){
			preHooks.add(indexToAddAt,(ContentletAPIPreHook)o);
		}else {
			throw new InstantiationException("This hook must implement ContentletAPIPrehook");
		}
	}

	@Override
    public void delPreHook ( int indexToRemAt ) {
        preHooks.remove( indexToRemAt );
    }

    @Override
    public void delPreHook ( Object preHook ) {
        preHooks.remove( preHook );
    }

    @Override
    public void delPreHookByClassName ( String className ) {

        Iterator<ContentletAPIPreHook> iterator = preHooks.iterator();

        while ( iterator.hasNext() ) {
            ContentletAPIPreHook hook = iterator.next();
            if ( className.equals( hook.getClass().getName() ) ) {
                iterator.remove();
            }
        }
    }

    @Override
    public void delPostHook ( int indexToRemAt ) {
        postHooks.remove( indexToRemAt );
    }

    @Override
    public void delPostHook ( Object postHook ) {
        postHooks.remove( postHook );
    }

    @Override
    public void delPostHookByClassName ( String className ) {
        Iterator<ContentletAPIPostHook> iterator = postHooks.iterator();

        while ( iterator.hasNext() ) {
            ContentletAPIPostHook hook = iterator.next();
            if ( className.equals( hook.getClass().getName() ) ) {
                iterator.remove();
            }
        }
    }

    @Override
	public List<String> getPreHooks() {
		List<String> result = new ArrayList<>();
		for (ContentletAPIPreHook hook : preHooks) {
			result.add(hook.getClass().getName());
		}
		return result;
	}

	@Override
	public List<String> getPostHooks() {
		List<String> result = new ArrayList<>();
		for (ContentletAPIPostHook hook : postHooks) {
			result.add(hook.getClass().getName());
		}
		return result;
	}

	@Override
	public long contentletCount() throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.contentletCount();
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		long c = conAPI.contentletCount();
		for(ContentletAPIPostHook post : postHooks){
			post.contentletCount(c);
		}
		return c;
	}

	@Override
	public long contentletIdentifierCount() throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.contentletIdentifierCount();
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		long c = conAPI.contentletIdentifierCount();
		for(ContentletAPIPostHook post : postHooks){
			post.contentletIdentifierCount(c);
		}
		return c;
	}

	@Override
	public void deleteAllVersionsandBackup(List<Contentlet> contentlets,
			User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotContentletStateException {
		// Not implemented
	}

	@Override
	public List<Contentlet> getSiblings(String identifier)
			throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){

			 boolean preResult = pre.getSiblings(identifier);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> contents= conAPI.getSiblings(identifier);
		for(ContentletAPIPostHook post : postHooks){
			post.getSiblings(identifier);
		}
		return contents;
	}

	@Override
	public List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,
			DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.DBSearch(query, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Map<String, Serializable>> c = conAPI.DBSearch(query, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.DBSearch(query, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public Contentlet copyContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyContentlet(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.copyContentlet(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.copyContentlet(contentlet, user, respectFrontendRoles, c);
		}
		return c;
	}

	@Override
	public Contentlet copyContentlet(Contentlet contentlet, Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyContentlet(contentlet, host, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.copyContentlet(contentlet, host, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.copyContentlet(contentlet, host, user, respectFrontendRoles, c);
		}
		return c;
	}

	@Override
	public Contentlet copyContentlet(final Contentlet contentlet, final ContentType contentType,
									 final Host site, final User user, final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotContentletStateException {
		for (final ContentletAPIPreHook preHook : this.preHooks) {
			final boolean preResult = preHook.copyContentlet(contentlet, contentType, site, user,
					respectFrontendRoles);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, preHook.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		final Contentlet returnedContent = this.conAPI.copyContentlet(contentlet, contentType, site, user,
				respectFrontendRoles);
		for (final ContentletAPIPostHook postHook : this.postHooks) {
			postHook.copyContentlet(contentlet, contentType, site, user, respectFrontendRoles, returnedContent);
		}
		return returnedContent;
	}

	@Override
	public Contentlet copyContentlet(Contentlet contentlet, Folder folder, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyContentlet(contentlet, folder, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.copyContentlet(contentlet, folder, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.copyContentlet(contentlet, folder, user, respectFrontendRoles, c);
		}
		return c;
	}

	@Override
	public Contentlet copyContentlet(final Contentlet contentlet, final ContentType contentType,
									 final Folder folder, final User user,
									 final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotContentletStateException {
		for (final ContentletAPIPreHook preHook : this.preHooks) {
			final boolean preResult = preHook.copyContentlet(contentlet, contentType, folder, user,
					respectFrontendRoles);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, preHook.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		final Contentlet returnedContent = this.conAPI.copyContentlet(contentlet, contentType,
				folder, user, respectFrontendRoles);
		for (final ContentletAPIPostHook postHook : this.postHooks) {
			postHook.copyContentlet(contentlet, contentType, folder, user, respectFrontendRoles,
					returnedContent);
		}
		return returnedContent;
	}

	@Override
	public Contentlet copyContentlet(Contentlet contentlet, Folder folder, User user, boolean appendCopyToFileName, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.copyContentlet(contentlet, folder, user, appendCopyToFileName, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet c = conAPI.copyContentlet(contentlet, folder, user, appendCopyToFileName, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.copyContentlet(contentlet, folder, user, appendCopyToFileName, respectFrontendRoles, c);
		}
		return c;
	}

	@Override
	public Contentlet copyContentlet(final Contentlet contentletToCopy,
									 final Host host, final Folder folder, final User user, final String copySuffix,
									 final boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException {

		for (ContentletAPIPreHook pre : preHooks) {

			final boolean preResult = pre.copyContentlet(contentletToCopy, host, folder, user, copySuffix, respectFrontendRoles);
			if(!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		final Contentlet copiedContentlet =
				this.conAPI.copyContentlet(contentletToCopy, host, folder, user, copySuffix, respectFrontendRoles);

		for(ContentletAPIPostHook post : postHooks) {
			post.copyContentlet(contentletToCopy, host, folder, user, copySuffix, respectFrontendRoles, copiedContentlet);
		}

		return copiedContentlet;
	}

	@Override
	public Contentlet copyContentlet(final Contentlet contentletToCopy,
									 final ContentType contentType, final Host site,
									 final Folder folder, final User user,
									 final String copySuffix, final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException, DotContentletStateException {
		for (final ContentletAPIPreHook pre : this.preHooks) {
			final boolean preResult = pre.copyContentlet(contentletToCopy, contentType, site, folder, user,
					copySuffix, respectFrontendRoles);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		final Contentlet copiedContentlet = this.conAPI.copyContentlet(contentletToCopy, contentType, site,
				folder, user, copySuffix, respectFrontendRoles);
		for (final ContentletAPIPostHook post : this.postHooks) {
			post.copyContentlet(contentletToCopy, contentType, site, folder, user, copySuffix,
					respectFrontendRoles, copiedContentlet);
		}
		return copiedContentlet;
	}

	@Override
	public boolean isInodeIndexed(String inode) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isInodeIndexed(inode);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isInodeIndexed(inode);
		for(ContentletAPIPostHook post : postHooks){
			post.isInodeIndexed(inode,c);
		}
		return c;
	}

	@Override
	public boolean isInodeIndexed(String inode, boolean live, boolean working) {
		for (ContentletAPIPreHook pre : preHooks) {
			boolean preResult = pre.isInodeIndexed(inode, live, working);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isInodeIndexed(inode, live, working);
		for (ContentletAPIPostHook post : postHooks) {
			post.isInodeIndexed(inode, live, working, c);
		}
		return c;
	}

	@Override
	public boolean isInodeIndexed(String inode,boolean live) {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.isInodeIndexed(inode,live);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        boolean c = conAPI.isInodeIndexed(inode,live);
        for(ContentletAPIPostHook post : postHooks){
            post.isInodeIndexed(inode,live,c);
        }
        return c;
    }

	@Override
	public boolean isInodeIndexed(String inode, boolean live, int secondsToWait) {
		for (ContentletAPIPreHook pre : preHooks) {
			boolean preResult = pre.isInodeIndexed(inode, live, secondsToWait);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isInodeIndexed(inode, live, secondsToWait);
		for (ContentletAPIPostHook post : postHooks) {
			post.isInodeIndexed(inode, live, secondsToWait, c);
		}
		return c;
	}

	@Override
	public boolean isInodeIndexed(String inode, int secondsToWait) {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.isInodeIndexed(inode,secondsToWait);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isInodeIndexed(inode,secondsToWait);
		for(ContentletAPIPostHook post : postHooks){
			post.isInodeIndexed(inode,secondsToWait,c);
		}
		return c;
	}

	@Override
    public boolean isInodeIndexedArchived(String inode){
        for(ContentletAPIPreHook pre : preHooks){
            final boolean preResult = pre.isInodeIndexedArchived(inode);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        boolean c = conAPI.isInodeIndexedArchived(inode);
        for(ContentletAPIPostHook post : postHooks){
            post.isInodeIndexedArchived(inode);
        }
        return c;
    }

	@Override
	public boolean isInodeIndexedArchived(String inode, int secondsToWait){
		for(ContentletAPIPreHook pre : preHooks){
			final boolean preResult = pre.isInodeIndexedArchived(inode, secondsToWait);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		boolean c = conAPI.isInodeIndexedArchived(inode, secondsToWait);
		for(ContentletAPIPostHook post : postHooks){
			post.isInodeIndexedArchived(inode, secondsToWait);
		}
		return c;
	}

	@Override
	public void UpdateContentWithSystemHost(String hostIdentifier)throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.UpdateContentWithSystemHost(hostIdentifier);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.UpdateContentWithSystemHost(hostIdentifier);
		for(ContentletAPIPostHook post : postHooks){
			post.UpdateContentWithSystemHost(hostIdentifier);
		}
	}

	@Override
	public void removeUserReferences(String userId)throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.removeUserReferences(userId);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.removeUserReferences(userId);
		for(ContentletAPIPostHook post : postHooks){
			post.removeUserReferences(userId);
		}
	}

	@Override
	public String getUrlMapForContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getUrlMapForContentlet(contentlet, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		String result = conAPI.getUrlMapForContentlet(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.getUrlMapForContentlet(contentlet, user, respectFrontendRoles);
		}

		return result;
	}

	@Override
	public void deleteVersion(Contentlet contentlet, User user,boolean respectFrontendRoles) throws DotDataException,DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.deleteVersion(contentlet,user,respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.deleteVersion(contentlet, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.deleteVersion(contentlet, user, respectFrontendRoles);
		}
	}

	@Override
	public Contentlet saveDraft(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException{
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.saveDraft(contentlet,contentRelationships, cats,permissions, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet savedContentlet = conAPI.saveDraft(contentlet,contentRelationships, cats,permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.saveDraft(savedContentlet,contentRelationships, cats,permissions, user, respectFrontendRoles);
		}

		return savedContentlet;
	}

	@Override
	public Contentlet saveDraft(Contentlet contentlet, ContentletRelationships contentletRelationships, List<Category> cats ,List<Permission> permissions, User user,boolean respectFrontendRoles) throws IllegalArgumentException,DotDataException,DotSecurityException, DotContentletStateException, DotContentletValidationException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.saveDraft(contentlet,contentletRelationships, cats,permissions, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Contentlet savedContentlet = conAPI.saveDraft(contentlet,contentletRelationships, cats,permissions, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.saveDraft(savedContentlet,contentletRelationships, cats,permissions, user, respectFrontendRoles);
		}

		return savedContentlet;
	}

	@Override
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles)	throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles);
		for(ContentletAPIPostHook post : postHooks){
			post.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission) throws DotDataException,	DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission);
		for(ContentletAPIPostHook post : postHooks){
			post.search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,c);
		}
		return c;
	}

	@Override
	public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset, String sortBy, User user, boolean respectFrontendRoles, int requiredPermission, boolean anyLanguage) throws DotDataException,	DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,anyLanguage);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Contentlet> c = conAPI.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,anyLanguage);
		for(ContentletAPIPostHook post : postHooks){
			post.searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,requiredPermission,anyLanguage);
		}
		return c;
	}

	@Override
	public void refreshContentUnderFolder(Folder folder)
			throws DotReindexStateException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.refreshContentUnderFolder(folder);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.refreshContentUnderFolder(folder);
		for(ContentletAPIPostHook post : postHooks){
			post.refreshContentUnderFolder(folder);
		}
	}

	@Override
	public void refreshContentUnderFolderPath ( String hostId, String folderPath ) throws DotReindexStateException {
		for ( ContentletAPIPreHook pre : preHooks ) {
			boolean preResult = pre.refreshContentUnderFolderPath(hostId, folderPath);
			if ( !preResult ) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.refreshContentUnderFolderPath(hostId, folderPath);
		for ( ContentletAPIPostHook post : postHooks ) {
			post.refreshContentUnderFolderPath(hostId, folderPath);
		}
	}

	@Override
	public void removeFolderReferences(Folder folder) throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.removeFolderReferences(folder);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.removeFolderReferences(folder);
		for(ContentletAPIPostHook post : postHooks){
			post.removeFolderReferences(folder);
		}
	}

	@Override
	public boolean canLock(Contentlet contentlet, User user) throws   DotLockException {
		boolean ret = true;
		for(ContentletAPIPreHook pre : preHooks){
			if(!pre.canLock(contentlet, user)){
				ret = false;
			}
		}
		if(!conAPI.canLock(contentlet, user)){
			ret = false;
		}
		for(ContentletAPIPostHook post : postHooks){
			if(!post.canLock(contentlet, user)){
				ret = false;
			}
		}
		return ret;
	}

	@Override
	public boolean canLock(Contentlet contentlet, User user, boolean respectFrondEnd) throws   DotLockException {
		boolean ret = true;
		for(ContentletAPIPreHook pre : preHooks){
			if(!pre.canLock(contentlet, user)){
				ret = false;
			}
		}
		if(!conAPI.canLock(contentlet, user, respectFrondEnd)){
			ret = false;
		}
		for(ContentletAPIPostHook post : postHooks){
			if(!post.canLock(contentlet, user)){
				ret = false;
			}
		}
		return ret;
	}

	@Override
	public Map<Relationship, List<Contentlet>> findContentRelationships(
			Contentlet contentlet, User user) throws DotDataException,
			DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.findContentRelationships(contentlet, user);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Map<Relationship, List<Contentlet>> c = conAPI.findContentRelationships(contentlet, user);
		for(ContentletAPIPostHook post : postHooks){
			post.findContentRelationships(contentlet, user);
		}
		return c;
	}

	@Override
    public Object loadField(String inode, Field field) throws DotDataException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.loadField(inode,field);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }

        Object value=conAPI.loadField(inode, field);

        for(ContentletAPIPostHook post : postHooks){
            post.loadField(inode,field,value);
        }
        return value;
    }

	@Override
	public Object loadField(String inode, com.dotcms.contenttype.model.field.Field field)
			throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.loadField(inode,field);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}

		Object value = conAPI.loadField(inode, field);

		for(ContentletAPIPostHook post : postHooks){
			post.loadField(inode, field ,value);
		}
		return value;
	}

	@Override
    public long indexCount(String luceneQuery, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.indexCount(luceneQuery,user,respectFrontendRoles);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }

        long value=conAPI.indexCount(luceneQuery, user, respectFrontendRoles);

        for(ContentletAPIPostHook post : postHooks){
            post.indexCount(luceneQuery,user,respectFrontendRoles,value);
        }
        return value;
    }

	@Override
	public List<Map<String, String>> getMostViewedContent(String structureVariableName,
			String startDate, String endDate, User user) {

		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.getMostViewedContent(structureVariableName, startDate, endDate, user);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		List<Map<String, String>> c = conAPI.getMostViewedContent(structureVariableName, startDate, endDate, user);
		for(ContentletAPIPostHook post : postHooks){
			post.getMostViewedContent(structureVariableName, startDate, endDate, user);
		}
		return c;
	}

    @Override
    public void publishAssociated(Contentlet contentlet, boolean isNew) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.publishAssociated(contentlet,isNew);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        conAPI.publishAssociated(contentlet,isNew);
        for(ContentletAPIPostHook post : postHooks){
            post.publishAssociated(contentlet,isNew);
        }
    }

    @Override
    public void publishAssociated(Contentlet contentlet, boolean isNew,  boolean isNewVersion) throws DotSecurityException, DotDataException, DotContentletStateException, DotStateException {
        for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.publishAssociated(contentlet,isNew,isNewVersion);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        conAPI.publishAssociated(contentlet,isNew,isNewVersion);
        for(ContentletAPIPostHook post : postHooks){
            post.publishAssociated(contentlet,isNew,isNewVersion);
        }
    }
    
    @Override
    public ESSearchResults esSearch(String esQuery, boolean live, User user,
    		boolean respectFrontendRoles) throws DotSecurityException,
    		DotDataException {
    	for(ContentletAPIPreHook pre : preHooks){
             boolean preResult = pre.esSearch(esQuery, live, user, respectFrontendRoles);
             if(!preResult){
				 String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				 Logger.error(this, errorMessage);
				 throw new DotRuntimeException(errorMessage);
             }
         }
    	 	ESSearchResults ret = conAPI.esSearch(esQuery, live, user, respectFrontendRoles);
         for(ContentletAPIPostHook post : postHooks){
             post.esSearchRaw(esQuery, live, user, respectFrontendRoles);
         }
         return ret;
    }

    @Override
    public SearchResponse esSearchRaw(String esQuery, boolean live, User user,
    		boolean respectFrontendRoles) throws DotSecurityException,
    		DotDataException {
		if (LicenseManager.getInstance().isCommunity()) {
			throw new DotStateException("Need an enterprise license to run this functionality.");
		}
    	for(ContentletAPIPreHook pre : preHooks){
            boolean preResult = pre.esSearchRaw(esQuery, live, user, respectFrontendRoles);
            if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
    	SearchResponse ret = conAPI.esSearchRaw(esQuery, live, user, respectFrontendRoles);
        for(ContentletAPIPostHook post : postHooks){
            post.esSearchRaw(esQuery, live, user, respectFrontendRoles);
        }
        return ret;
    }

	@Override
	public void updateUserReferences(User userToReplace, String replacementUserId, User user)
			throws DotDataException, DotSecurityException {
		for(ContentletAPIPreHook pre : preHooks){
			boolean preResult = pre.updateUserReferences(userToReplace,replacementUserId, user);
			if(!preResult){
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		conAPI.updateUserReferences(userToReplace,replacementUserId, user);
		for(ContentletAPIPostHook post : postHooks){
			post.updateUserReferences(userToReplace,replacementUserId, user);
		}
	}

	@Override
	public int updateModDate(final Set<String> inodes, final User user) throws DotDataException {
		for(ContentletAPIPreHook pre : preHooks){
			pre.updateModDate(inodes);
		}
		final int num = conAPI.updateModDate(inodes, user);
		for(ContentletAPIPostHook post : postHooks){
			post.updateModDate(inodes, user);
		}
		return num;
	}

    @Override
    public Optional<Contentlet> findContentletByIdentifierOrFallback(String identifier, boolean live, long incomingLangId, User user,
            boolean respectFrontendRoles) {
        for (ContentletAPIPreHook pre : preHooks) {
            boolean preResult = pre.findContentletByIdentifierOrFallback(identifier, live, incomingLangId, user, respectFrontendRoles);
            if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        Optional<Contentlet> savedContentlet =
                conAPI.findContentletByIdentifierOrFallback(identifier, live, incomingLangId, user, respectFrontendRoles);
        for (ContentletAPIPostHook post : postHooks) {
            post.findContentletByIdentifierOrFallback(identifier, live, incomingLangId, user, respectFrontendRoles);
        }

        return savedContentlet;
    }

	@Override
	public Optional<Contentlet> findContentletByIdentifierOrFallback(String identifier, boolean live, long incomingLangId, User user,
																	 boolean respectFrontendRoles, String variantName) {
		for (ContentletAPIPreHook pre : preHooks) {
			boolean preResult = pre.findContentletByIdentifierOrFallback(identifier, live, incomingLangId, user, respectFrontendRoles, variantName);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		Optional<Contentlet> savedContentlet =
				conAPI.findContentletByIdentifierOrFallback(identifier, live, incomingLangId, user, respectFrontendRoles, variantName);
		for (ContentletAPIPostHook post : postHooks) {
			post.findContentletByIdentifierOrFallback(identifier, live, incomingLangId, user, respectFrontendRoles, variantName);
		}

		return savedContentlet;
	}

	@Override
	public Optional<Contentlet> findContentletByIdentifierOrFallback(String identifier, long incomingLangId,
			String variantId, Date timeMachine, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		for (ContentletAPIPreHook pre : preHooks) {
			boolean preResult = pre.findContentletByIdentifierOrFallback(identifier, incomingLangId,
					variantId, timeMachine, user, respectFrontendRoles);
			if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
			}
		}
		final Optional<Contentlet> found =
				conAPI.findContentletByIdentifierOrFallback(identifier, incomingLangId, variantId,
						timeMachine, user, respectFrontendRoles);
		for (ContentletAPIPostHook post : postHooks) {
			post.findContentletByIdentifierOrFallback(identifier, incomingLangId, variantId,
					timeMachine, user, respectFrontendRoles);
		}

		return found;
	}


    @Override
    public Optional<Contentlet> findInDb(String inode) {
        for (ContentletAPIPreHook pre : preHooks) {
            boolean preResult = pre.findInDb(inode);
            if (!preResult) {
				String errorMessage = String.format(PREHOOK_FAILED_MESSAGE, pre.getClass().getName());
				Logger.error(this, errorMessage);
				throw new DotRuntimeException(errorMessage);
            }
        }
        Optional<Contentlet> savedContentlet =
                conAPI.findInDb(inode);
        for (ContentletAPIPostHook post : postHooks) {
            post.findInDb(inode);
        }

        return savedContentlet;
    }

}
