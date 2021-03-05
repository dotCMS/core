import { ActionHeaderDeleteOptions } from './action-header-delete-options.model';

export interface ButtonModel {
    command: any;
    deleteOptions?: ActionHeaderDeleteOptions;
    icon?: string;
    isDelete?: boolean;
    label: string;
}
