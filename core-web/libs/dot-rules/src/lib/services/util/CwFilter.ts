import { Verify } from '../validation/Verify';

const numberRegex = /[\d.]/;
export class CwFilter {
    static transformValue(fieldValue: any): any {
        let xform = fieldValue;
        if (Verify.exists(fieldValue)) {
            if (fieldValue === 'true') {
                xform = true;
            } else if (fieldValue === 'false') {
                xform = false;
            } else if (fieldValue.match(numberRegex)) {
                xform = Number.parseFloat(fieldValue);
            }
        }
        return xform;
    }

    static isFiltered(obj: any, filterText: string): boolean {
        let isFiltered = false;
        if (filterText !== '') {
            let filter = filterText;
            let re = /([\w]*[:][\w]*)/g;
            let matches = filterText.match(re);
            if (matches != null) {
                // 'match' is now an array of the field filters.
                matches.forEach((match) => {
                    let terms = match.split(':');
                    filter = filter.replace(match, '');
                    if (!isFiltered) {
                        let fieldName = terms[0];
                        let fieldValue = terms[1];
                        let hasField =
                            obj.hasOwnProperty(fieldName) || obj.hasOwnProperty('_' + fieldName);
                        if (hasField) {
                            try {
                                isFiltered =
                                    obj[fieldName] !== fieldValue &&
                                    obj[fieldName] !== this.transformValue(fieldValue);
                            } catch (e) {
                                // tslint:disable-next-line:no-console
                                console.log(
                                    'Error while trying to check a field value while filtering.',
                                    e
                                );
                            }
                        }
                    }
                });
            }
            filter = filter.trim().toLowerCase();
            if (filter) {
                isFiltered = isFiltered || !(obj.name.toLowerCase().indexOf(filter) >= 0);
            }
        }
        return isFiltered;
    }
}
