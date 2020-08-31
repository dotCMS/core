import { E2EElement, E2EPage } from '../stencil.core/testing';
export declare const dotTestUtil: {
    getDotLabel: (page: E2EPage) => Promise<E2EElement>;
    getHint: (page: E2EPage) => Promise<E2EElement>;
    getErrorMessage: (page: E2EPage) => Promise<E2EElement>;
    class: {
        empty: string[];
        emptyPristineInvalid: string[];
        emptyRequired: string[];
        emptyRequiredPristine: string[];
        filled: string[];
        filledRequired: string[];
        filledRequiredPristine: string[];
        touchedPristine: string[];
    };
    triggerStatusChange: (pristine: boolean, touched: boolean, valid: boolean, element: E2EElement, isValidRange?: boolean) => void;
};
