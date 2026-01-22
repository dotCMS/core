import { Verify } from './verify.util';

const numberRegex = /[\d.]/;

/**
 * Utility class for filtering rules based on field values and text search
 */
export class RuleFilter {
    static transformValue(fieldValue: unknown): unknown {
        let xform = fieldValue;
        if (Verify.exists(fieldValue)) {
            if (fieldValue === 'true') {
                xform = true;
            } else if (fieldValue === 'false') {
                xform = false;
            } else if (typeof fieldValue === 'string' && fieldValue.match(numberRegex)) {
                xform = Number.parseFloat(fieldValue);
            }
        }

        return xform;
    }

    static isFiltered(obj: { name: string }, filterText: string): boolean {
        let isFiltered = false;
        if (filterText !== '') {
            let filter = filterText;
            const re = /([\w]*[:][\w]*)/g;
            const matches = filterText.match(re);
            if (matches != null) {
                matches.forEach((match) => {
                    const terms = match.split(':');
                    filter = filter.replace(match, '');
                    if (!isFiltered) {
                        const fieldName = terms[0];
                        const fieldValue = terms[1];
                        const hasField =
                            Object.prototype.hasOwnProperty.call(obj, fieldName) ||
                            Object.prototype.hasOwnProperty.call(obj, '_' + fieldName);
                        if (hasField) {
                            try {
                                isFiltered =
                                    obj[fieldName] !== fieldValue &&
                                    obj[fieldName] !== this.transformValue(fieldValue);
                            } catch (e) {
                                console.error(
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

/** @deprecated Use RuleFilter instead */
export const CwFilter = RuleFilter;
