package com.dotcms.rest.api.v1.system;

import com.dotcms.business.SystemTable;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.WhiteBlackList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSet;
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
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This Jersey end-point provides access to the system table.
 * This is a public endpoint and requires Admin authentication
 *
 * @author jsanca
 */
@Path("/v1/system-table")
@SuppressWarnings("serial")
public class SystemTableResource implements Serializable {

	private final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();
	private final WhiteBlackList whiteBlackList = new WhiteBlackList.Builder()
							.addWhitePatterns(Config.getStringArrayProperty("SYSTEM_TABLE_WHITELISTED_KEYS",
									new String[]{"^DOT_.*"}))
							.addBlackPatterns(Config.getStringArrayProperty("SYSTEM_TABLE_BLACKLISTED_KEYS",
									new String[]{"SYSTEM_TABLE_BLACKLISTED_KEYS","SYSTEM_TABLE_WHITELISTED_KEYS"})).build();

	/**
	 * Returns all entries in the system table.
	 * @param request
	 * @param response
	 * @param key
	 * @return
	 * @throws IOException
	 */
	@GET
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final ResponseEntityView<Map<String,String>> getAll(@Context final HttpServletRequest request,
															   @Context final HttpServletResponse response)
			throws IllegalAccessException {

		this.init(request, response);
		final Map<String, String> allEntries = this.systemTable.findAll();
		final Map<String, String> filteredEntries = allEntries.entrySet().stream()
				.filter(entry -> this.whiteBlackList.isAllowed(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Logger.debug(this, ()-> "Getting all system table values");
		return new ResponseEntityView<Map<String, String>>(filteredEntries);
	}

	/**
	 * Find a value in the system table by key, 404 if not found
	 * @param request
	 * @param response
	 * @param key
	 * @return
	 * @throws IOException
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

		this.init(request, response);
		this.checkBlackList(key);

		Logger.debug(this, ()-> "Getting system table value for key: " + key);
		final Optional<String> valueOpt = this.systemTable.find(key);
		if (valueOpt.isPresent()) {

			return new ResponseEntityStringView(valueOpt.get());
		}

		throw new DoesNotExistException("Key not found: " + key);
	}

	private void init(final HttpServletRequest request, final HttpServletResponse response) {
		new InitBuilder(request, response)
				.requiredBackendUser(true)
				.requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();
	}

	private void checkBlackList(final String key) throws IllegalArgumentException {

		if (!this.whiteBlackList.isAllowed(key)) {
			Logger.debug(this, ()-> "Key is blacklisted: " + key);
			throw new IllegalArgumentException("Key not allowed: " + key);
		}
	}

	/**
	 * Saves a value to the system table
	 * @param request
	 * @param response
	 * @param form
	 * @return
	 * @throws IllegalAccessException
	 */
	@POST
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public ResponseEntityStringView save(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			final KeyValueForm form) throws IllegalAccessException {

		this.init(request, response);
		this.checkBlackList(form.getKey());

		Logger.debug(this, ()-> "Saving system table value for key: " + form.getKey());
		this.systemTable.save(form.getKey(), form.getValue());
		
		return new ResponseEntityStringView(form.getKey() + " Saved");
	}

	/**
	 * Updates a value in the system table
	 * 404 if the value to update does not exist
	 *
	 * @param request
	 * @return
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

		this.init(request, response);
		this.checkBlackList(form.getKey());

		final Optional<String> valueOpt = this.systemTable.find(form.getKey());
		if (!valueOpt.isPresent()) {

			throw new DoesNotExistException("Key not found: " + form.getKey());
		}

		Logger.debug(this, ()-> "Updating system table value for key: " + form.getKey());
		this.systemTable.update(form.getKey(), form.getValue());

		return new ResponseEntityStringView(form.getKey() + " Updated");
	}

	/**
	 * Deletes a value in the system table
	 * 404 if the value to update does not exist
	 *
	 * @param request
	 * @return
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
			@PathParam("key") final String key) throws IllegalAccessException {

		this.init(request, response);
		this.checkBlackList(key);

		final Optional<String> valueOpt = this.systemTable.find(key);
		if (!valueOpt.isPresent()) {

			throw new DoesNotExistException("Key not found: " + key);
		}

		Logger.debug(this, ()-> "Deleting system table value for key: " + key);
		this.systemTable.delete(key);

		return new ResponseEntityStringView(key + " Deleted");
	}

}
