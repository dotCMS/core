/**
 * Loads the vendored Monaco editor (html/assets/monaco-editor) through Dojo's AMD loader for the
 * legacy content editor, and exposes it as `window.monaco` for the web components that reuse it.
 *
 * Monaco 0.55+ ships an AMD build that needs a few adjustments to run under Dojo:
 * - it only publishes the global `monaco` when MonacoEnvironment.globalAPI is set;
 * - its nls loader plugin reads a RequireJS-style config argument that Dojo does not pass, so it
 *   is replaced by a stub that keeps the default (English) UI strings;
 * - editor.main replaces MonacoEnvironment with workers that load their script through a blob +
 *   importScripts, a request /html/* rejects, so each worker is started from its file URL instead.
 *   The hashed file names below belong to the vendored Monaco version and must be updated with it.
 */
(function () {
    var WORKER_PATH = '/html/assets/monaco-editor/min/vs/assets/';
    var WORKERS = {
        json: 'json.worker-DKiEKt88.js',
        css: 'css.worker-HnVq6Ewq.js',
        scss: 'css.worker-HnVq6Ewq.js',
        less: 'css.worker-HnVq6Ewq.js',
        html: 'html.worker-B51mlPHg.js',
        handlebars: 'html.worker-B51mlPHg.js',
        razor: 'html.worker-B51mlPHg.js',
        typescript: 'ts.worker-CMbG-7ft.js',
        javascript: 'ts.worker-CMbG-7ft.js'
    };
    var EDITOR_WORKER = 'editor.worker-Be8ye1pW.js';

    window.MonacoEnvironment = { globalAPI: true };

    define('vs/nls.messages-loader', [], {
        load: function (id, req, loaded) {
            loaded({});
        }
    });

    require(['vs/editor/editor.main'], function () {
        window.MonacoEnvironment.getWorker = function (moduleId, label) {
            return new Worker(WORKER_PATH + (WORKERS[label] || EDITOR_WORKER));
        };
        // Hack to avoid MonacoEditorLoaderService to load the editor again
        // That service not works in `dojo` environment Dojo amdLoader. See docs: [https://dojotoolkit.org/reference-guide/1.7/loader/amd.html?highlight=packages%20location%20name%20main]
        window.monacoEditorAlreadyInitialized = !!window.monaco;
    });
})();
