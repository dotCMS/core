import { SpectatorOverrides } from '@openng/spectator';

/**
 * Props for a component whose signal inputs are aliased.
 *
 * Spectator keys `props` (and `setInput`) by the class *property* name — `$field` — while the
 * `ComponentRef.setInput` it calls underneath needs the public *alias*, `field`. A component that
 * follows this repo's `$name` + `{ alias }` convention therefore cannot express its inputs through
 * `props` at all: the name that type-checks throws at runtime, and the name that works does not
 * type-check.
 *
 * This states that gap once, so specs do not each carry an `as unknown` cast. It keeps the props
 * applied before the first change-detection pass, which `createComponent({ props })` guarantees and
 * a `setInput` after construction does not.
 */
export const aliasedProps = <C>(props: Record<string, unknown>): SpectatorOverrides<C>['props'] =>
    props as unknown as SpectatorOverrides<C>['props'];
