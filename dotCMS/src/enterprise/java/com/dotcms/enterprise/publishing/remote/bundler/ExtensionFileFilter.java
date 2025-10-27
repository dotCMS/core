package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.publishing.BundlerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * Tak
 */
public class ExtensionFileFilter implements java.io.FileFilter {


    final String[] extensions;

    final boolean readXML = BundlerUtil.readXML();
    final boolean readJSON = BundlerUtil.readJSON();

    public ExtensionFileFilter(String... extensions) {
        this.extensions = extensions;
    }
    public ExtensionFileFilter(String[]... extensionsArrays) {
        List<String> extensionList = new ArrayList<>();
        for(String[] exts:extensionsArrays){
            extensionList.addAll(Stream.of(exts).filter(e->e!=null).collect(java.util.stream.Collectors.toList()));
        }
        this.extensions = extensionList.toArray(new String[0]);
    }

    public boolean accept(java.io.File file) {
        if (file.isDirectory()) {
            return true;
        }
        for (String extension : extensions) {

            if (readJSON && extension.endsWith(".json") && file.getName().toLowerCase()
                    .endsWith(extension.toLowerCase())) {
                return true;

            }
            if (readXML && extension.endsWith(".xml") && file.getName().toLowerCase()
                    .endsWith(extension.toLowerCase())) {
                return true;
            }

        }
        return false;

    }

}
