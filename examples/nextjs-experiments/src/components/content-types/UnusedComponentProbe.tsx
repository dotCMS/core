/**
 * A mapped content type that no page in the demo site uses.
 *
 * It exists so the bundle checks have something deterministic to look for: because it is
 * reachable only through `next/dynamic`, the marker below must not appear anywhere in the
 * initial route's JavaScript. If someone converts the component map back to static imports,
 * `scripts/check-initial-bundle.mjs` finds the marker and fails.
 *
 * Keep the marker string literal and unique — do not build it from parts, or minification
 * may split it and the check will stop detecting anything.
 */
export const UNUSED_COMPONENT_MARKER = "__dotcms_unused_component_probe__";

export default function UnusedComponentProbe() {
    return <div data-testid={UNUSED_COMPONENT_MARKER}>{UNUSED_COMPONENT_MARKER}</div>;
}
