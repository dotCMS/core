/**
 * A bubble form's values, keyed by control key. Strings for text and number controls,
 * booleans for checkboxes — mirroring `DynamicControl<string | boolean>`.
 */
export type BubbleFormValues = Record<string, string | boolean>;

/** What the form stream emits. `null` means it was dismissed without applying. */
export type BubbleFormValue = BubbleFormValues | null;

export interface DynamicControl<T> {
    value?: T;
    key?: string;
    label?: string;
    required?: boolean;
    controlType?: string;
    type?: string;
    min?: number;
}
