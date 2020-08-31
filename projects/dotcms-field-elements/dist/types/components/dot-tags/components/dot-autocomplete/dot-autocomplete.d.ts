import '../../../../stencil.core';
import { EventEmitter } from '../../../../stencil.core';
export declare class DotAutocompleteComponent {
    el: HTMLElement;
    /** (optional) Disables field's interaction */
    disabled: boolean;
    /** (optional) text to show when no value is set */
    placeholder: string;
    /** (optional)  Min characters to start search in the autocomplete input */
    threshold: number;
    /** (optional)  Max results to show after a autocomplete search */
    maxResults: number;
    /** (optional) Duraction in ms to start search into the autocomplete */
    debounce: number;
    /** Function or array of string to get the data to use for the autocomplete search */
    data: () => Promise<string[]> | string[];
    selection: EventEmitter<string>;
    enter: EventEmitter<string>;
    lostFocus: EventEmitter<FocusEvent>;
    private readonly id;
    private keyEvent;
    componentDidLoad(): void;
    render(): JSX.Element;
    watchThreshold(): void;
    watchData(): void;
    watchMaxResults(): void;
    private handleKeyDown;
    private handleBlur;
    private clean;
    private cleanOptions;
    private emitselect;
    private emitEnter;
    private getInputElement;
    private initAutocomplete;
    private clearList;
    private focusOnInput;
    private getResultList;
    private getResultListId;
    private getData;
}
