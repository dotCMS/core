/*
  Current state of the UVE
*/
export interface UVEState {
    mode: UVE_MODE;
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
