import { ActionHeaderOptionsPrimary } from './action-header-options-primary.model';
import { ButtonAction } from './button-action.model';

export interface ActionHeaderOptions {
    primary?: ActionHeaderOptionsPrimary;
    secondary?: ButtonAction[];
}
