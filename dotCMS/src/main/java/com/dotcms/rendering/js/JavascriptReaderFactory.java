package com.dotcms.rendering.js;

/**
 * Factory to create a File Js Reader or RequestBody Js Reader
 * @author jsanca
 */
class JavascriptReaderFactory {

    private JavascriptReaderFactory() {}

    static JavascriptReader getJavascriptReader(final boolean folderExists) {
        return folderExists?
             new FileJavascriptReader():
             new RequestBodyJavascriptReader();
    }
}
