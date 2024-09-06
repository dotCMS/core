package com.dotcms.csspreproc;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

/**
 * Provides important utility methods and specifies the basic behavior for CSS Compilers in dotCMS.
 * <p>This abstract class makes sure that dotCMS has all the dependent SCSS Files required for a successful SCSS
 * compilation. Therefore, it's very important that all related files are correctly scanned so their dependencies can
 * be found and referenced as expected.</p>
 *
 * @author Will Ezell
 * @since Jul 26th, 2019
 */
abstract class DotCSSCompiler {

  protected final HttpServletRequest req;
  protected final Set<String> allImportedURI = new HashSet<>();
  protected byte[] output;
  protected final String inputURI;
  protected final Host inputHost;
  protected final boolean inputLive;

  public Set<String> getAllImportedURI() {
    return allImportedURI;
  }

  public byte[] getOutput() {
    return output;
  }

  public DotCSSCompiler(final Host host, final String uri, final boolean live, final HttpServletRequest req) {
    inputHost = host;
    inputURI = uri;
    inputLive = live;
    this.req = req;
  }

  public abstract void compile() throws DotSecurityException, DotStateException, DotDataException, IOException;

  /**
   * Reads the content of the specified SCSS File in order to extract the paths specified by the {@code @import}
   * directive. Such paths will be used by dotCMS to read and import the associated SCSS files for SASS to compile them
   * correctly. This method uses a special Regular Expression to extract folder and file paths from the {@code @import}
   * directive, so please take this into consideration in case a given path is not read correctly.
   *
   * @param file The SCSS File containing potential imports.
   *
   * @return The list of SCSS Files and/or paths that must be included by dotCMS during the compilation process.
   *
   * @throws IOException An error occurred when reading the SCSS File being imported.
   */
  protected List<String> getImportedUris(final File file) throws IOException {
    final List<String> imported = new ArrayList<>();
    final String pattern = "^@import\\s+(\\(\\w+\\)\\s+)?(url\\(\\s*)?((\"(?<f1>[^\"]+)\")|('(?<f2>[^']+)'))";
    // Use the RegEx to collect the file/folder paths from the @import directive
    final Pattern cssImport = Pattern.compile(pattern);
    final BufferedReader bin = new BufferedReader(new FileReader(file));
    String line;
    while ((line = bin.readLine()) != null) {
      line = line.trim();
      if (!UtilMethods.isSet(line) || !line.startsWith("@import")) {
        continue;
      }
      final Matcher matcher = cssImport.matcher(line);
      while (matcher.find()) {
        try {
          String importUrl = matcher.group("f1");
          if (UtilMethods.isSet(importUrl)) {
            imported.add(importUrl);
          } else {
            // If RegEx group "f1" is empty, get group "f2" instead
            importUrl = matcher.group("f2");
            if (UtilMethods.isSet(importUrl)) {
              imported.add(importUrl);
            } else {
              Logger.warn(this, String.format("Import directive from file '%s' could not be parsed!", file.getAbsolutePath()));
            }
          }
        } catch (final Exception ex) {
          Logger.error(this, String.format("An error occurred when extracting CSS import from '%s', line [ %s ]: %s",
                  file.getAbsolutePath(), line, ex.getMessage()), ex);
        }
      }
    }
    bin.close();
    return imported;
  }

  protected List<String> filterImportedUris(String baseUri, List<String> uris) throws DotSecurityException {

    List<String> filtered = new ArrayList<>(uris.size());
    List<String> baseParts = new ArrayList<>(Arrays.<String>asList(baseUri.split("/")));
    baseParts.remove(baseParts.size() - 1);
    while (uris.remove(null));
    for (String uri : uris) {
      if (uri.startsWith("/")) {
        filtered.add(uri);
      } else {
        // relative path processing
        LinkedList<String> xparts = new LinkedList<>(baseParts);
        for (String part : uri.split("/")) {
          if (part.equals("..")) {
            if (!baseParts.isEmpty()) {
              xparts.removeLast(); // back to parent directory
            } else {
              throw new DotSecurityException("trying to read files outside allowed uris");
            }
          } else if (!part.equals(".") && part.length() > 0)
            xparts.addLast(part);// follow relative path
        }
        StringBuilder uriStr = new StringBuilder();
        for (String part : xparts) {
          if (part.length() > 0) {
            uriStr.append("/").append(part);
          }
        }
        if (uriStr.length() > 0) {
          filtered.add(uriStr.toString());
        }
      }
    }

    return filtered;
  }

  protected abstract String addExtensionIfNeeded(String url);

  public abstract String getDefaultExtension();

  protected File createCompileDir(Host currentHost, String uri, boolean live)
      throws IOException, DotStateException, DotDataException, DotSecurityException {

    File compDir = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + File.separator + "css_compile_space" + File.separator
        + UUIDGenerator.generateUuid());
    compDir.mkdirs();
    LinkedList<String> uriq = new LinkedList<>();
    uriq.add(uri);
    while (!uriq.isEmpty()) {
      String fileuri = addExtensionIfNeeded(uriq.removeFirst());
      String fileuricopy = fileuri;
      // check if it is a path to other host
      Host host;
      if (fileuri.startsWith("//")) {
        String hostName = fileuri.substring(2, fileuri.indexOf('/', 2));
        host = APILocator.getHostAPI().findByName(hostName, APILocator.getUserAPI().getSystemUser(), false);
        fileuri = fileuri.substring(fileuri.indexOf('/', 2));
      } else {
        host = currentHost;
      }

      Identifier ident = APILocator.getIdentifierAPI().find(host, fileuri);
      if (ident == null || !InodeUtils.isSet(ident.getId())) {
        fileuri = addImportUnderscore(fileuri);
        fileuricopy = fileuri;
        ident = APILocator.getIdentifierAPI().find(host, fileuri);
      }
      if (ident != null && InodeUtils.isSet(ident.getId())) {
        Contentlet file = APILocator.getContentletAPI().findContentletByIdentifier(ident.getId(), live,
            APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
        if (file != null && InodeUtils.isSet(file.getInode())) {
          File ff = file.getBinary(FileAssetAPI.BINARY_FIELD);
          if (ff != null && ff.isFile()) {

            // destfile: /../../../tmpdir/host/path/to/asset.extension

            if (!fileuricopy.equals(uri)) {
              if (host.equals(currentHost)) {
                allImportedURI.add(fileuricopy);
              } else {
                allImportedURI.add("//" + host.getHostname() + fileuri);
              }
            }

            List<String> imports = getImportedUris(ff);
            uriq.addAll(filterImportedUris(fileuri, imports));

            File dest =
                new File(compDir.getAbsolutePath() + File.separator + host.getHostname() + fileuri.replaceAll("\\\\", File.separator));
            dest.getParentFile().mkdirs();

            boolean hasAbs = false;
            for (String imp : imports) {
              hasAbs = hasAbs || (imp.charAt(0) == '/');
            }

            if (hasAbs) {
              BufferedReader in = new BufferedReader(new FileReader(ff));
              BufferedWriter out = new BufferedWriter(new FileWriter(dest));
              try {
                String line;
                while ((line = in.readLine()) != null) {
                  if (line.contains("@import")) {
                    int idx = line.indexOf("@import");
                    while (idx < line.length() && line.charAt(idx) != '\'' && line.charAt(idx) != '"') {
                      idx++;
                    }
                    idx++;
                    if (line.charAt(idx) == '/') {
                      boolean whost = (line.charAt(idx + 1) == '/'); // path with host?

                      line = line.substring(0, idx) // until the quote
                          + compDir.getAbsolutePath() + File.separator // compile dir path
                          + (!whost ? host.getHostname() + File.separator : "") // add host name if needed
                          + line.substring(idx + (whost ? 2 : 1), line.length()); // the rest ignoring the / or //
                    }
                  }

                  out.write(line);
                }
              } finally {
                out.close();
                in.close();
              }
            } else {
              FileUtil.copyFile(ff, dest);
            }
          }
        }
      }

      
      // copy any files that did not make it
      final String inode = live ? "live_inode" : "working_inode";
      final StringBuilder sqlQuery = new StringBuilder("select cvi." + inode +" as inode from contentlet_version_info cvi, identifier id where"
              + " id.parent_path like ? and id.host_inode = ? and cvi.identifier = id.id");
      final List<Object> parameters = new ArrayList<>();
      parameters.add(ident.getParentPath() + StringPool.PERCENT);
      parameters.add(ident.getHostId());

      final DotConnect dc = new DotConnect().setSQL(sqlQuery.toString());
      parameters.forEach(param -> dc.addParam(param));

      final List<Map<String,String>> inodesMapList =  dc.loadResults();

      final List<String> inodes = new ArrayList<>();
      for (final Map<String, String> versionInfoMap : inodesMapList) {
        inodes.add(versionInfoMap.get("inode"));
      }

      List<Contentlet> contentletList = APILocator.getContentletAPI().findContentlets(inodes).stream()
              .filter(contentlet -> contentlet.getBaseType().get().ordinal() == BaseContentType.FILEASSET.ordinal())
              .filter(c -> Try.of(() -> !c.isArchived()).getOrElse(false))
              .collect(Collectors.toList());

      for (final Contentlet con : contentletList) {
        final FileAsset asset = APILocator.getFileAssetAPI().fromContentlet(con);
        final String assetUri = asset.getURI();
        final File f = new File(compDir.getAbsolutePath() + File.separator + inputHost.getHostname() + asset.getPath() + File.separator + asset.getFileName());
        if (f.exists() || (assetUri.endsWith(".scss") && StringUtils.shareSamePath(uri, assetUri)) || UtilMethods.isEmpty(asset::getFileAsset))  {
          Logger.debug(this.getClass(),"Skipping asset:" + asset.getURI());
          continue;
        }
        getAllImportedURI().add(assetUri);
        f.getParentFile().mkdirs();
        FileUtil.copyFile(asset.getFileAsset(), f);
      }

    }

    return compDir;
  }

  private String addImportUnderscore(String url) {
    return url.substring(0, url.lastIndexOf('/')) + "/_" + url.substring(url.lastIndexOf('/') + 1, url.length());
  }
}
