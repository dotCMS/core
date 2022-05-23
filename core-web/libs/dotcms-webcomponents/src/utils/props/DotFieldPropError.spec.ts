import { PropValidationInfo } from './models';

import DotFieldPropError from './DotFieldPropError';

describe('DotFieldPropError', () => {
    const propInfo: PropValidationInfo<string> = {
        field: { type: 'test-type', name: 'field-name' },
        name: 'test-name',
        value: 'test-value'
    };

    const warningText = `Warning: Invalid prop "${propInfo.name}" of type "${typeof propInfo.value}" supplied to "${propInfo
        .field.type}" with the name "${propInfo.field.name}", expected "TEST".
Doc Reference: https://github.com/dotCMS/core-web/blob/master/projects/dotcms-field-elements/src/components/${propInfo
        .field.type}/readme.md`;

    it('should throw Warning exception with the correct information', () => {
        expect(() => {
            throw new DotFieldPropError(propInfo, 'TEST');
        }).toThrowError(warningText);
    });
});
