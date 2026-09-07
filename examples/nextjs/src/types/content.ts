import type {
  DotCMSBasicContentlet,
  DotCMSNavigationItem,
} from "@dotcms/types";

/**
 * Shared domain types for the example.
 *
 * Content-type components receive a dotCMS contentlet as props. The SDK ships
 * `DotCMSBasicContentlet` with the common system fields (identifier, inode,
 * dotStyleProperties, …); here we extend it with the custom fields each
 * content type adds, so components stay typed without resorting to `any`.
 */

/** Toggle group set by editors (e.g. `{ bold: true, italic: false }`). */
export type StyleFlags = Record<string, boolean>;

/**
 * Per-field styling overrides set by editors in the UVE. Most fields are a
 * Tailwind-class token; the `*-style`/`*-effects` fields are flag maps. Typed
 * with the known keys so component reads don't need casting.
 */
export interface DotStyleProperties {
  "title-size"?: string;
  "title-style"?: StyleFlags;
  "caption-size"?: string;
  "description-size"?: string;
  "text-alignment"?: string;
  "overlay-style"?: string;
  "image-height"?: string;
  "card-background"?: string;
  "card-effects"?: StyleFlags;
  "border-radius"?: string;
  "button-color"?: string;
  "button-size"?: string;
  "button-style"?: StyleFlags;
  layout?: string;
  // Allow other editor-defined keys while keeping the known ones typed; also
  // keeps this assignable to the SDK's `Record<string, unknown>` field.
  [key: string]: string | StyleFlags | undefined;
}

/** A dotCMS image/file reference as returned by the page and GraphQL APIs. */
export interface DotCMSImage {
  identifier?: string;
  idPath?: string;
  fileName?: string;
}

/** Base type for every content-type component's props. */
export interface ContentTypeProps extends DotCMSBasicContentlet {
  dotStyleProperties?: DotStyleProperties;
}

/** Author shape returned by the Blog GraphQL fragment. */
export interface BlogAuthor {
  firstName?: string;
  lastName?: string;
  inode?: string;
}

/** Blog contentlet shape returned by `blogQuery`. */
export interface Blog {
  title: string;
  identifier: string;
  inode?: string;
  image?: DotCMSImage | string;
  urlMap?: string;
  modDate?: string;
  urlTitle?: string;
  teaser?: string;
  author?: BlogAuthor;
}

/** Destination contentlet shape returned by `destinationQuery`. */
export interface Destination {
  title: string;
  identifier: string;
  inode?: string;
  image?: DotCMSImage | string;
  url?: string;
  urlMap?: string;
  modDate?: string;
  shortDescription?: string;
  selectValue?: string;
  activities?: string[];
}

/** Navigation node returned by `navigationQuery`. */
export type NavItem = DotCMSNavigationItem;

/** Extra GraphQL content composed into the page response by `getDotCMSPage`. */
export interface PageExtraContent {
  blogs?: Blog[];
  destinations?: Destination[];
  navigation?: NavItem;
}
