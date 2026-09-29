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
 * (core-web/libs/sdk/react/src/lib/next/components/Contentlet/Contentlet.tsx).
 * Both constants MUST have the same value ('dotcms-contentlet') for analytics
 * tracking to work correctly with React-rendered contentlets.
 *
 * This duplication is intentional to maintain SDK independence:
 * - @dotcms/analytics can be used standalone without React
 * - @dotcms/react can be used without analytics
 * - When both are used together, they must share the same class name
 *
 * If you need to change this value, you MUST update it in both locations:
 * 1. This file (analytics SDK)
 * 2. Contentlet.tsx in React SDK
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
