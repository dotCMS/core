/*
 * Constants the content trackers share: how contentlets are found, and the rescan event.
 */

/**
 * Default debounce time in milliseconds for MutationObserver
 */
export const CONTENTLET_OBSERVER_DEBOUNCE_MS = 250;

/**
 * CSS class selector for contentlet elements
 *
 * @important This constant is intentionally duplicated in @dotcms/react SDK
 * (core-web/libs/sdk/react/src/lib/next/components/Contentlet/Contentlet.tsx), whose renderers
 * print the class, and dotCMS prints it on traditional pages too. Both constants MUST have the
 * same value ('dotcms-contentlet') for content tracking to find React-rendered contentlets.
 *
 * This duplication is intentional to keep the SDKs independent:
 * - @dotcms/events works without React
 * - @dotcms/react works without events
 * - When both are used together, they must share the same class name
 *
 * If you need to change this value, you MUST update it in both locations:
 * 1. This file (@dotcms/events)
 * 2. Contentlet.tsx in @dotcms/react
 *
 * @see core-web/libs/sdk/react/src/lib/next/components/Contentlet/Contentlet.tsx
 */
export const CONTENTLET_CLASS = 'dotcms-contentlet';

/**
 * Attribute that carries a contentlet's identifier (`dataset['dotIdentifier']`). The page
 * renderers print it only once analytics is active, after hydration, so it can land after
 * the contentlet itself.
 */
export const CONTENTLET_IDENTIFIER_ATTRIBUTE = 'data-dot-identifier';

/**
 * Window event that asks the content trackers to scan for contentlets again. Sent when
 * contentlets that were hidden become visible, which changes no DOM node the trackers watch
 * (an experiment's rows are revealed through a style rule).
 */
export const CONTENTLET_RESCAN_EVENT = 'dotcms:events:rescan';
