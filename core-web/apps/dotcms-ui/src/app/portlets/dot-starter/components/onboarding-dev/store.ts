import { signalState } from '@ngrx/signals';

type OnBoardingState = {
    progress: number;
    activeAccordionIndex: number;
    currentStateLabel: string;
};

const INITIAL_STATE: OnBoardingState = {
    progress: 0,
    activeAccordionIndex: 0,
    currentStateLabel: ''
};

export const state = signalState<OnBoardingState>(INITIAL_STATE);
