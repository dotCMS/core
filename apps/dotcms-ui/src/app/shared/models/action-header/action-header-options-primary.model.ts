import { ButtonModel } from './button.model';

export interface ActionHeaderOptionsPrimary {
    command?: (event?: any) => void;
    model?: ButtonModel[];
}
