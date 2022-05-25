import { ActionHeaderDeleteOptions } from './action-header-delete-options.model';

export interface ButtonModel {
    command: (options?: unknown) => void;
    deleteOptions?: ActionHeaderDeleteOptions;
    icon?: string;
    isDelete?: boolean;
    label: string;
}
