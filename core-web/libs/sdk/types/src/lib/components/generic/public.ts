export const DotCMSEntityState = {
    IDLE: 'IDLE',
    LOADING: 'LOADING',
    SUCCESS: 'SUCCESS',
    ERROR: 'ERROR'
} as const;

export type DotCMSEntityState =
    | { state: typeof DotCMSEntityState.IDLE }
    | { state: typeof DotCMSEntityState.LOADING }
    | { state: typeof DotCMSEntityState.SUCCESS }
    | { state: typeof DotCMSEntityState.ERROR; error: Error };
