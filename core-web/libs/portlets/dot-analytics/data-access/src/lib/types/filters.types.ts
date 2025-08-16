import { TimeRange } from "./common.types";

export interface FilterOption {
    /** Display text for the option */
    label: string;
    /** Internal value for the option */
    value: TimeRange;
}
