// Only the component map is re-exported.
//
// This barrel used to `export *` from every content-type component, which made all of them
// statically reachable from anything importing it — enough on its own to pull every
// component into the initial bundle no matter how `dotComponents` loads them. Import a
// component directly from its own module if you need it somewhere else.
export { dotComponents } from "./dotComponents";
