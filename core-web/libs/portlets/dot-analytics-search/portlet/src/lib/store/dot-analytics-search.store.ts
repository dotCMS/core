import { signalStore, withState } from '@ngrx/signals';

export type DotContentAnalyticsState = {
    isEnterprise: boolean;
};

export const initialState: DotContentAnalyticsState = {
    isEnterprise: false
};

export const DotAnalyticsSearchStore = signalStore(withState(initialState));
