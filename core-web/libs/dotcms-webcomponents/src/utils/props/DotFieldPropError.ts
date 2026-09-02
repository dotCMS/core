import { PropValidationInfo } from './models/PropValidationInfo';

export default class DotFieldPropError<T> extends Error {
    private readonly propInfo: PropValidationInfo<T>;

    constructor(propInfo: PropValidationInfo<T>, expectedType: string) {
        super(
            `Warning: Invalid prop "${
                propInfo.name
            }" of type "${typeof propInfo.value}" supplied to "${
                propInfo.field.type
            }" with the name "${propInfo.field.name}", expected "${expectedType}".
Doc Reference: https://github.com/dotCMS/core-web/blob/main/libs/dotcms-webcomponents/src/components/contenttypes-fields/${
                propInfo.field.type
            }/readme.md`
        );
        this.propInfo = propInfo;
    }

    getProps() {
        return { ...this.propInfo };
    }
}
