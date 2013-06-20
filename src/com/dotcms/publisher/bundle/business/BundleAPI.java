package com.dotcms.publisher.bundle.business;

import java.util.List;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.exception.DotDataException;

public interface BundleAPI {

	/**
	 * persists the given Bundle object to the underlying data layer.
	 *
	 * @param	bundle	the bundle to be persisted
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void saveBundle(Bundle bundle)  throws DotDataException;

	/**
	 * persists the given Bundle object, and sets the given Environment List as the destination environments
	 * of the bundle.
	 *
	 * The environments included in the given List must already exist, they will not be created.
	 *
	 * @param	bundle	the bundle to be persisted
	 * @param	environments	the list of destination environments
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void saveBundle(Bundle bundle, List<Environment> environments)  throws DotDataException;

	/**
	 * persists the given Environment as a destination of the given Bundle.
	 *
	 * Both the environment and the bundle must exist, they will not be created.
	 *
	 * @param	bundle	the bundle whose destination the given environment will be added to
	 * @param	environments	the environment to be saved as destination of the given bundle
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void saveBundleEnvironment(Bundle b, Environment e) throws DotDataException;

	/**
	 * persists the given Environment List as a destination of the given Bundle
	 *
	 * Both the environments in the List and the bundle must exist, they will not be created.
	 *
	 * @param	bundle	the bundle whose destination the given environments will be added to
	 * @param	environments	the environments to be saves as destinations of the given bundle
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void saveBundleEnvironments(Bundle b, List<Environment> envs) throws DotDataException;

}
