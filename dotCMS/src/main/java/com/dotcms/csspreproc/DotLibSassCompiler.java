package com.dotcms.csspreproc;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.OutputStyle;
import java.util.Optional;

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
            final URI inputFile = compileTargetFile.toURI();
            final URI outputFile = compileDestinationFile.toURI();

            final io.bit3.jsass.Compiler compiler = new io.bit3.jsass.Compiler();
            final Options options = new Options();
            final int SASS_OUTPUTSTYLE_ORDINAL = Config.getIntProperty("LIBSASS_OUTPUTSTYLE", OutputStyle.COMPRESSED.ordinal());
            options.setOutputStyle(OutputStyle.values()[SASS_OUTPUTSTYLE_ORDINAL]);
            final Output out = compiler.compileFile(inputFile, outputFile, options);

            this.output = out.getCss().getBytes();

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

    @Override
    protected String addExtensionIfNeeded(String url) {
        return url.indexOf('.', url.lastIndexOf('/')) != -1 ? url: url + ".scss";
    }

    @Override
    public String getDefaultExtension() {
        return "scss";
    }

}
