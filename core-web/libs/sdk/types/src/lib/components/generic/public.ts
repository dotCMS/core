export type DotCMSEntityStatus =
    | { state: 'idle' }
    | { state: 'loading' }
    | { state: 'success' }
    | { state: 'error'; error: Error };
