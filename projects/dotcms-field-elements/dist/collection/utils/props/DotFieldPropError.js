export default class DotFieldPropError extends Error {
    constructor(propInfo, expectedType) {
        super(`Warning: Invalid prop "${propInfo.name}" of type "${typeof propInfo.value}" supplied to "${propInfo.field.type}" with the name "${propInfo.field.name}", expected "${expectedType}".
Doc Reference: https://github.com/dotCMS/core-web/blob/master/projects/dotcms-field-elements/src/components/${propInfo.field.type}/readme.md`);
        this.propInfo = propInfo;
    }
    getProps() {
        return Object.assign({}, this.propInfo);
    }
}
