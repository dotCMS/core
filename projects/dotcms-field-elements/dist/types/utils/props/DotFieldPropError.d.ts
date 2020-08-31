import { PropValidationInfo } from './models/PropValidationInfo';
export default class DotFieldPropError<T> extends Error {
    private readonly propInfo;
    constructor(propInfo: PropValidationInfo<T>, expectedType: string);
    getProps(): {
        field: {
            type: string;
            name: string;
        };
        name: string;
        value: T;
    };
}
