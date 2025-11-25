package com.dotcms.rest.api.v1.system;

import com.dotcms.business.SystemTable;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.WhiteBlackList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotcms.util.DotPreconditions.checkNotNull;

/**
 * This Jersey end-point provides access to the system table.
 * This is a public endpoint and requires Admin authentication
 *
 * @author jsanca
 */
@Path("/v1/system-table")
public class SystemTableResource implements Serializable {

	private final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();
	private static final String[] DEFAULT_BLACKLISTED_PROPS = new String[]{"DOTCMS_CLUSTER_ID", "DOTCMS_CLUSTER_SALT"};
	private final WhiteBlackList whiteBlackList = new WhiteBlackList.Builder()
							.addWhitePatterns(Config.getStringArrayProperty("SYSTEM_TABLE_WHITELISTED_KEYS",
									new String[]{StringPool.BLANK}))
			.addBlackPatterns(CollectionsUtils.concat(Config.getStringArrayProperty(
					"SYSTEM_TABLE_BLACKLISTED_KEYS", new String[]{}), DEFAULT_BLACKLISTED_PROPS)).build();

	/**
	 * Returns all entries in the system table.
	 *
	 * @param request  The current instance of the {@link HttpServletRequest}.
	 * @param response The current instance of the {@link HttpServletResponse}.
	 *
	 * @return A {@link Map} containing all entries in the system table.
	 */
	@GET
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final ResponseEntityView<Map<String,String>> getAll(@Context final HttpServletRequest request,
															   @Context final HttpServletResponse response) {

		this.init(request, response);
		final Map<String, String> allEntries = this.systemTable.all();
		final Map<String, String> filteredEntries = allEntries.entrySet().stream()
				.filter(entry -> this.whiteBlackList.isAllowed(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Logger.debug(this, ()-> "Getting all system table values");
		return new ResponseEntityView<>(filteredEntries);
	}

	/**
	 * Returns the value of a key in the System Table, or returns a 404 if not found.
	 *
	 * @param request  The current instance of the {@link HttpServletRequest}.
	 * @param response The current instance of the {@link HttpServletResponse}.
	 * @param key      The key to search for.
	 *
	 * @return The value for the key.
	 *
	 * @throws IllegalArgumentException If the key is blacklisted.
	 * @throws DoesNotExistException The key was not found.
	 */
	@Path("/{key}")
	@GET
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final ResponseEntityStringView get(@Context final HttpServletRequest request,
											 @Context final HttpServletResponse response,
											 @PathParam("key") final String key)
			throws IllegalAccessException {
		checkNotEmpty(key, IllegalArgumentException.class, "Key cannot be null or empty");
		this.init(request, response);
		this.checkBlackList(key);

		Logger.debug(this, ()-> "Getting system table value for key: " + key);
		final Optional<String> valueOpt = this.systemTable.get(key);
		if (valueOpt.isPresent()) {

			return new ResponseEntityStringView(valueOpt.get());
		}

		throw new DoesNotExistException("Key not found: " + key);
	}

	/**
	 * Defines the required information that must be available in the request for this REST Endpoint
	 * to provide the expected information. In this case, the User in the request must have back-end
	 * access permissions, and must be a CMS Administrator.
	 *
	 * @param request  The current instance of the {@link HttpServletRequest}.
	 * @param response The current instance of the {@link HttpServletResponse}.
	 */
	private void init(final HttpServletRequest request, final HttpServletResponse response) {
		new InitBuilder(request, response)
				.requiredBackendUser(true)
				.requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();
	}

	/**
	 * Checks if the key is blacklisted.
	 *
	 * @param key The key to check.
	 *
	 * @throws IllegalArgumentException If the key is blacklisted.
	 */
	private void checkBlackList(final String key) throws IllegalArgumentException {

		if (!this.whiteBlackList.isAllowed(key)) {
			Logger.debug(this, ()-> "Key is blacklisted: " + key);
			throw new IllegalArgumentException("Key not allowed: " + key);
		}
	}

	/**
	 * Saves or updates the value of a given key to the system table.
	 *
	 * @param request  The current instance of the {@link HttpServletRequest}.
	 * @param response The current instance of the {@link HttpServletResponse}.
	 * @param form     The {@link KeyValueForm} object containing the key and value to save.
	 *
	 * @return The key that was saved.
	 *
	 * @throws IllegalArgumentException If the key is blacklisted.
	 */
	@POST
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public ResponseEntityStringView save(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			final KeyValueForm form) throws IllegalAccessException {
		checkNotNull(form, IllegalArgumentException.class, "KeyValueForm cannot be null");
		this.init(request, response);
		this.checkBlackList(form.getKey());
		Logger.debug(this, ()-> "Saving/updating system table value for key: " + form.getKey());
		this.systemTable.set(form.getKey(), form.getValue());
		
		return new ResponseEntityStringView(form.getKey() + " saved/updated");
	}

	/**
	 * Updates a value in the System Table, or returns a 404 if the key doesn't exist.
	 *
	 * @param request The current instance of the {@link HttpServletRequest}.
	 *
	 * @return The key that was updated.
	 *
	 * @throws IllegalArgumentException If the key is blacklisted.
	 */
	@PUT
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public ResponseEntityStringView update(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			final KeyValueForm form) throws IllegalAccessException {
		checkNotNull(form, IllegalArgumentException.class, "KeyValueForm cannot be null");
		this.init(request, response);
		this.checkBlackList(form.getKey());

		final Optional<String> valueOpt = this.systemTable.get(form.getKey());
		if (valueOpt.isEmpty()) {
			throw new DoesNotExistException("Key not found: " + form.getKey());
		}

		Logger.debug(this, ()-> "Updating system table value for key: " + form.getKey());
		this.systemTable.set(form.getKey(), form.getValue());

		return new ResponseEntityStringView(form.getKey() + " Updated");
	}

	/**
	 * Deletes a value from the System Table, or returns a 404 if the key doesn't exist.
	 *
	 * @param request The current instance of the {@link HttpServletRequest}.
	 *
	 * @return The key that was deleted.
	 *
	 * @throws IllegalArgumentException If the key is blacklisted.
	 */
	@DELETE
	@Path("/{key}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public ResponseEntityStringView delete(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@PathParam("key") final String key)  {
		return deleteWithKey(request, response, Map.of("key",key));
	}

	/**
	 * Deletes a value from the System Table, or returns a 404 if the key doesn't exist.
	 *
	 * @param request The current instance of the {@link HttpServletRequest}.
	 *
	 * @return The key that was deleted.
	 *
	 * @throws IllegalArgumentException If the key is blacklisted.
	 */
	@DELETE
	@Path("/_delete")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public ResponseEntityStringView deleteWithKey(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			final Map<String,String> keyMap) {
		if(UtilMethods.isEmpty(()->keyMap.get("key"))){
			throw new IllegalArgumentException("Key cannot be null or empty");
		}
		String key = keyMap.get("key");

		this.init(request, response);
		this.checkBlackList(key);

		final Optional<String> valueOpt = this.systemTable.get(key);
		if (valueOpt.isEmpty()) {
			throw new DoesNotExistException("Key not found: " + key);
		}

		Logger.debug(this, ()-> "Deleting system table value for key: " + key);
		this.systemTable.delete(key);

		return new ResponseEntityStringView(key + " Deleted");
	}

}
