package com.dotcms.csspreproc;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.csspreproc.dartsass.DartSassCompiler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RuntimeUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 *
 */
public class DotLibSassCompiler extends DotCSSCompiler {

    final boolean live;

    public DotLibSassCompiler(final Host host, final String uri, final boolean live) {
        super(host, uri, live);
        this.live = live;
    }

    @Override
    public void compile() throws DotSecurityException, DotStateException, DotDataException, IOException {

        String trying = inputURI.substring(0, inputURI.lastIndexOf('.')) + ".scss";
        File compileDir  = createCompileDir( inputHost, trying, inputLive );

        try {
            // replace the extension .css with .scss


            Identifier ident = APILocator.getIdentifierAPI().find(inputHost, trying);
            if (ident == null || null == ident.getId()) {
                trying = inputURI.substring(0, inputURI.lastIndexOf('.')) + ".sass";
                ident = APILocator.getIdentifierAPI().find(inputHost, trying);
                if (ident == null || null == ident.getId()) {
                    throw new IOException("file: " + inputHost.getHostname() + ":" + trying + " not found");
                }
            }

            final Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(),
                    APILocator.getLanguageAPI().getDefaultLanguage().getId());

            if(!info.isPresent()) {
                throw new DotDataException("Can't find Contentlet-version-info. Identifier: " + ident.getId() + ". Lang:"
                        + APILocator.getLanguageAPI().getDefaultLanguage().getId());
            }

            final FileAsset mainFile = APILocator.getFileAssetAPI()
                .fromContentlet(APILocator.getContentletAPI().find(info.get().getWorkingInode(),
                        APILocator.systemUser(), true));

            // build directories to build scss
            final File compileTargetFile =
                    new File(compileDir.getAbsolutePath() + File.separator + inputHost.getHostname() +
                            File.separator + mainFile.getPath() + File.separator + mainFile.getFileName());
            if (!compileTargetFile.exists()) {
                final String errorMsg = String.format("Compile SCSS file '%s' does not exist or cannot be read. Please " +
                        "check that the path to the file has the correct format.", compileTargetFile.getAbsolutePath());
                throw new RuntimeException(errorMsg);
            }
            final File compileDestinationFile = new File(compileTargetFile.getAbsoluteFile() + ".css");
            final DartSassCompiler compiler = new DartSassCompiler(compileTargetFile, compileDestinationFile);
            final Optional<String> out = compiler.compile();
            handleOutput(compiler.terminalOutput());
            this.output = out.get().getBytes();
        } catch (final Exception ex) {
            final String errorMsg = "Unable to compile SASS code in " + inputHost.getHostname() + ":" + inputURI + " live:" + inputLive;
            Logger.error(this, errorMsg, ex);
            throw new RuntimeException(errorMsg, ex);
        } finally {
          DotConcurrentFactory.getInstance().getSubmitter().submit(() -> {
            FileUtil.deltree(compileDir);
          });
        }
    }

    /**
     *
     * @param terminalOutput
     */
    private void handleOutput(final RuntimeUtils.TerminalOutput terminalOutput) {
        if (0 == terminalOutput.exitValue() && UtilMethods.isSet(terminalOutput.output())) {
            Logger.warn(this, terminalOutput.output());
            notifyUI(terminalOutput.output(), MessageSeverity.WARNING);
        } else if (UtilMethods.isSet(terminalOutput.output())) {
            Logger.error(this, terminalOutput.output());
            notifyUI(terminalOutput.output(), MessageSeverity.ERROR);
        }
    }

    /**
     *
     * @param message
     * @param severity
     */
    private void notifyUI(final String message, final MessageSeverity severity) {
        final SystemMessageBuilder messageBuilder =
                new SystemMessageBuilder().setMessage(UtilMethods.htmlLineBreak(message)).setType(MessageType.SIMPLE_MESSAGE).setSeverity(severity).setLife(10000);
        final String loggedInUserID =
                WebAPILocator.getUserWebAPI().getLoggedInUser(HttpServletRequestThreadLocal.INSTANCE.getRequest()).getUserId();
        SystemMessageEventUtil.getInstance().pushMessage(messageBuilder.create(), ImmutableList.of(loggedInUserID));
    }

    @Override
    protected String addExtensionIfNeeded(String url) {
        return url.indexOf('.', url.lastIndexOf('/')) != -1 ? url: url + ".scss";
    }

    @Override
    public String getDefaultExtension() {
        return "scss";
    }

}
