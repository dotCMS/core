package com.dotcms.rendering.js;

class JavascriptReaderFactory {

    private JavascriptReaderFactory() {}

    static JavascriptReader getJavascriptReader(final boolean folderExists) {
        return folderExists?
             new FileJavascriptReader():
             new RequestBodyJavascriptReader();
    }
}
