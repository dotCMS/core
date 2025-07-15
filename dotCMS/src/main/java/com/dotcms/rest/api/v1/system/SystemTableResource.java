package com.dotcms.rest.api.v1.system;

import com.dotcms.business.SystemTable;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityMapStringStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.WhiteBlackList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@SwaggerCompliant(value = "System administration and configuration APIs", batch = 4)
@Tag(name = "System Configuration")
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
	@Operation(
		summary = "Get all system table entries",
		description = "Returns all entries in the system table (filtered by whitelist/blacklist)"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "System table entries retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityMapStringStringView.class))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - CMS Administrator role required",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON})
	public final ResponseEntityView<Map<String,String>> getAll(@Context final HttpServletRequest request,
															   @Context final HttpServletResponse response) {

		this.init(request, response);
		final Map<String, String> allEntries = this.systemTable.all();
		final Map<String, String> filteredEntries = allEntries.entrySet().stream()
				.filter(entry -> this.whiteBlackList.isAllowed(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Logger.debug(this, ()-> "Getting all system table values");
		return new ResponseEntityMapStringStringView(filteredEntries);
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
	@Operation(
		summary = "Get system table value by key",
		description = "Returns the value of a key in the System Table"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "System table value retrieved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityStringView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - key is blacklisted or invalid",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - CMS Administrator role required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Key not found",
					content = @Content(mediaType = "application/json"))
	})
	@Path("/{key}")
	@GET
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON})
	public final ResponseEntityStringView get(@Context final HttpServletRequest request,
											 @Context final HttpServletResponse response,
											 @Parameter(description = "System table key", required = true)
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
	@Operation(
		summary = "Save system table key-value",
		description = "Saves or updates the value of a given key to the system table"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "System table value saved successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityStringView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - key is blacklisted or form is invalid",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - CMS Administrator role required",
					content = @Content(mediaType = "application/json"))
	})
	@POST
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON})
	public ResponseEntityStringView save(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Key-value form data", 
				required = true,
				content = @Content(schema = @Schema(implementation = KeyValueForm.class))
			)
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
	@Operation(
		summary = "Update system table key-value",
		description = "Updates a value in the System Table, returns 404 if the key doesn't exist"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "System table value updated successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityStringView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - key is blacklisted or form is invalid",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - CMS Administrator role required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Key not found",
					content = @Content(mediaType = "application/json"))
	})
	@PUT
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON})
	public ResponseEntityStringView update(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Key-value form data", 
				required = true,
				content = @Content(schema = @Schema(implementation = KeyValueForm.class))
			)
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
	@Operation(
		summary = "Delete system table key",
		description = "Deletes a value from the System Table, returns 404 if the key doesn't exist"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "System table key deleted successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityStringView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - key is blacklisted or invalid",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - CMS Administrator role required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Key not found",
					content = @Content(mediaType = "application/json"))
	})
	@DELETE
	@Path("/{key}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON})
	public ResponseEntityStringView delete(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@Parameter(description = "System table key to delete", required = true)
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
	@Operation(
		summary = "Delete system table key with request body",
		description = "Deletes a value from the System Table using key in request body, returns 404 if the key doesn't exist"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "System table key deleted successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(implementation = ResponseEntityStringView.class))),
		@ApiResponse(responseCode = "400", 
					description = "Bad request - key is blacklisted or invalid",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - CMS Administrator role required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "404", 
					description = "Key not found",
					content = @Content(mediaType = "application/json"))
	})
	@DELETE
	@Path("/_delete")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_JSON})
	public ResponseEntityStringView deleteWithKey(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "Map containing the key to delete", 
				required = true,
				content = @Content(schema = @Schema(type = "object"))
			)
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
