import { ButtonModel } from './button.model';

export interface ActionHeaderOptionsPrimary {
    command?: (event?: unknown) => void;
    model?: ButtonModel[];
}
