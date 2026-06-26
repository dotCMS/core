import type { DotCMSBasicContentlet } from "@dotcms/types";

/**
 * Shared domain types for the example.
 *
 * Content-type components receive a dotCMS contentlet as props. The SDK ships
 * `DotCMSBasicContentlet` with the common system fields (identifier, inode,
 * dotStyleProperties, …); here we extend it with the custom fields each
 * content type adds, so components stay typed without resorting to `any`.
 */

/** Free-form, per-field styling overrides set by editors in the UVE. */
export type DotStyleProperties = Record<string, unknown>;

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

/** Navigation node returned by `navigationQuery` (mirrors DotCMSNavigationItem). */
export interface NavItem {
  code?: string;
  folder: string;
  hash?: number;
  host?: string;
  href: string;
  languageId?: number;
  order?: number;
  target?: string;
  title: string;
  type?: string;
  children?: NavItem[];
}

/** Extra GraphQL content composed into the page response by `getDotCMSPage`. */
export interface PageExtraContent {
  blogs?: Blog[];
  destinations?: Destination[];
  navigation?: NavItem;
}
