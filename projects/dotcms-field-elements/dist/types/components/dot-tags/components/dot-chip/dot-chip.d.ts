import '../../../../stencil.core';
import { EventEmitter } from '../../../../stencil.core';
export declare class DotChipComponent {
    el: HTMLElement;
    /** Chip's label */
    label: string;
    /** (optional) Delete button's label */
    deleteLabel: string;
    /** (optional) If is true disabled the delete button */
    disabled: boolean;
    remove: EventEmitter<String>;
    render(): JSX.Element;
}
