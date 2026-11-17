package com.ettrema.http.report;

import com.bradmcevoy.http.ExistingEntityHandler;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.ReportableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceHandlerHelper;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.dotmarketing.util.Logger;
import java.util.Map;

/**
 * dotCMS's replacement for the WebDAV library's REPORT handler, under the same class name.
 *
 * <p>The library (milton 1.8.1.4) builds this handler whenever it builds its WebDAV protocol, and
 * its own version parses the request body with jdom 1.x, so the whole WebDAV servlet fails to start
 * when jdom 1.x is absent. This class sits in {@code WEB-INF/classes}, which the web application
 * class loader searches before {@code WEB-INF/lib}, so the library links against it instead.
 *
 * <p>Behaviour is the library's for dotCMS: REPORT is offered only for resources that implement
 * {@link ReportableResource}, which no dotCMS resource does, so clients get "not implemented"
 * exactly as before. A resource that did implement it is not reachable in dotCMS; this class
 * answers it with 400 Bad Request without reading the body.
 */
public class ReportHandler implements ExistingEntityHandler {

    private final ResourceHandlerHelper resourceHandlerHelper;

    /**
     * Matches the constructor the library's WebDAV protocol calls.
     *
     * @param responseHandler unused; kept for the library's call
     * @param resourceHandlerHelper the shared request sequence (resource lookup, authorisation)
     * @param reports unused; reports need jdom 1.x types and dotCMS registers none
     */
    public ReportHandler(final WebDavResponseHandler responseHandler,
            final ResourceHandlerHelper resourceHandlerHelper, final Map<String, Report> reports) {
        this.resourceHandlerHelper = resourceHandlerHelper;
    }

    /** @return the REPORT method, as the library's handler declares */
    @Override
    public String[] getMethods() {
        return new String[]{Method.REPORT.code};
    }

    /** Runs the library's shared sequence, which ends in {@link #processResource}. */
    @Override
    public void process(final HttpManager manager, final Request request, final Response response)
            throws ConflictException, NotAuthorizedException, BadRequestException {
        resourceHandlerHelper.process(manager, request, response, this);
    }

    /** Runs the library's shared per-resource checks, which end in {@link #processExistingResource}. */
    @Override
    public void processResource(final HttpManager manager, final Request request,
            final Response response, final Resource resource)
            throws NotAuthorizedException, ConflictException, BadRequestException {
        resourceHandlerHelper.processResource(manager, request, response, resource, this);
    }

    /**
     * Answers 400 Bad Request without reading the body: no report is registered, so there is
     * nothing the body could name that would be answered.
     */
    @Override
    public void processExistingResource(final HttpManager manager, final Request request,
            final Response response, final Resource resource) throws BadRequestException {
        Logger.debug(ReportHandler.class, "REPORT is not supported; answering bad request");
        throw new BadRequestException(resource);
    }

    /** @return whether the resource accepts REPORT, the library's test */
    @Override
    public boolean isCompatible(final Resource resource) {
        return resource instanceof ReportableResource;
    }
}
