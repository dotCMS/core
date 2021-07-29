package com.dotcms.csspreproc;

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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.FileUtil;

abstract class DotCSSCompiler {
  protected final Set<String> allImportedURI = new HashSet<String>();
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

  public DotCSSCompiler(Host host, String uri, boolean live) {
    inputHost = host;
    inputURI = uri;
    inputLive = live;
  }

  public abstract void compile() throws DotSecurityException, DotStateException, DotDataException, IOException;

  protected List<String> getImportedUris(File file) throws IOException {

    List<String> imported = new ArrayList<String>();

    String pattern = "^@import\\s+(\\(\\w+\\)\\s+)?(url\\(\\s*)?((\"(?<f1>[^\"]+)\")|('(?<f2>[^']+)'))";

    // collect the file names
    Pattern cssImport = Pattern.compile(pattern);
    BufferedReader bin = new BufferedReader(new FileReader(file));
    String line;
    while ((line = bin.readLine()) != null) {
      Matcher matcher = cssImport.matcher(line);
      while (matcher.find()) {
        try {
          imported.add(matcher.group("f1"));
        } catch (Exception ex) {
          try {
            imported.add(matcher.group("f2"));
          } catch (Exception exx) {
          }
        }
      }
    }

    bin.close();
    return imported;
  }

  protected List<String> filterImportedUris(String baseUri, List<String> uris) throws DotSecurityException {

    List<String> filtered = new ArrayList<String>(uris.size());
    List<String> baseParts = new ArrayList<String>(Arrays.<String>asList(baseUri.split("/")));
    baseParts.remove(baseParts.size() - 1);
    while (uris.remove(null));
    for (String uri : uris) {
      if (uri.startsWith("/")) {
        filtered.add(uri);
      } else {
        // relative path processing
        LinkedList<String> xparts = new LinkedList<String>(baseParts);
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
    LinkedList<String> uriq = new LinkedList<String>();
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
      final String query = "+path:" + ident.getParentPath() + "* +baseType:" + BaseContentType.FILEASSET.ordinal() + " +conHost:"
          + ident.getHostId() + ((live) ? " +live:true" : " +working:true");
      final List<Contentlet> files = APILocator.getContentletAPI().search(query, 1000, 0, null, APILocator.systemUser(), true);
      for (Contentlet con : files) {
        FileAsset asset = APILocator.getFileAssetAPI().fromContentlet(con);
        File f = new File(
            compDir.getAbsolutePath() + File.separator + inputHost.getHostname() + asset.getPath() + File.separator + asset.getFileName());
        if (f.exists())
          continue;
        String assetUri = asset.getURI();
        if (assetUri.endsWith(".scss") && areSiblings(uri, assetUri)) {
          continue;
        }
        getAllImportedURI().add(assetUri);
        f.getParentFile().mkdirs();
        FileUtil.copyFile(asset.getFileAsset(), f);
      }

    }

    return compDir;
  }

  private boolean areSiblings(final String uri, final String otherUri) {
    return uri.substring(0, uri.lastIndexOf("/")).equals(otherUri.substring(0, otherUri.lastIndexOf("/")));
  }

  private String addImportUnderscore(String url) {
    return url.substring(0, url.lastIndexOf('/')) + "/_" + url.substring(url.lastIndexOf('/') + 1, url.length());
  }
}
