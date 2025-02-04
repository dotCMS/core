/*
 Current state of the UVE
 * - mode: The current editor mode (preview, edit, live)
 * - languageId: The language ID of the current page setted on the UVE
 * - persona: The persona of the current page setted on the UVE
 * - variantName: The name of the current variant
 * - experimentId: The ID of the current experiment
 * - publishDate: The publish date of the current page setted on the UVE
*/
export interface UVEState {
    mode: UVE_MODE;
    persona: string | null;
    variantName: string | null;
    experimentId: string | null;
    publishDate: string | null;
    languageId: string | null;
}

/*
  Possible modes of UVE
  LIVE = You will see published and future content
  PREVIEW = You will see published and working content
  EDIT = UVE is available to edit content
  UNKNOWN = Used to catch possible errors, UVE should not end in this mode
*/
export enum UVE_MODE {
    EDIT = 'edit',
    PREVIEW = 'preview',
    LIVE = 'live',
    UNKNOWN = 'unknown'
}
