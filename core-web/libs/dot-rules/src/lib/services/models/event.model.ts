/**
 * Event model for form changes
 */
export interface ChangeEvent {
    valid: boolean;
    isBlur: boolean;
}

/** @deprecated Use ChangeEvent instead */
export type CwChangeEvent = ChangeEvent;
