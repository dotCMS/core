/**
 * The script dotCMS injects into traditional pages, built as an IIFE by `build:standalone`
 * (`ca.min.js`, which dotCMS serves at `/ext/analytics/ca.min.js`). It is not a package export:
 * it reads its config from its own script tag, starts `events`, and plain scripts on the page
 * reach the SDK as `window.dotEvents`. It runs the page's experiments too, from the contentlet
 * wrappers dotCMS prints, in place of dotCMS's own experiments script.
 */

import { initEvents } from './lib/events';
import { SCRIPT_SELECTOR, readScriptConfig } from './lib/standalone/config';

const current = document.currentScript;
const script =
    current instanceof HTMLScriptElement && current.hasAttribute('data-analytics-auth')
        ? current
        : document.querySelector<HTMLScriptElement>(SCRIPT_SELECTOR);
const config = readScriptConfig(script, window.location.origin);

if (config) {
    // Traditional pages carry dotCMS's contentlet wrappers, not the experiment markup
    initEvents(config, 'contentlets');
} else {
    console.warn(
        '[dotCMS events] the script tag has no data-analytics-auth, so events did not start'
    );
}
