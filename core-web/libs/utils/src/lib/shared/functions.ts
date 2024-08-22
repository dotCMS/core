// Replacement for lodash.isEmpty
// https://gist.github.com/inPhoenix/45a9f9e2568126d206f1125caebcd122
export const isEmpty = (value: unknown): boolean => {
    return (
        value == null || // From standard.js: Always use === - but obj == null is allowed to check null || undefined
        (typeof value === 'object' && Object.keys(value).length === 0) || // This catches arrays and objects
        (typeof value === 'string' && value.trim().length === 0)
    );
};

// Replacement for lodash.isEqual
// https://gist.github.com/jsjain/a2ba5d40f20e19f734a53c0aad937fbb
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const isEqual = (first: any, second: any): boolean => {
    if (first === second) {
        return true;
    }

    if (
        (first === undefined || second === undefined || first === null || second === null) &&
        (first || second)
    ) {
        return false;
    }

    const firstType = first?.constructor.name;
    const secondType = second?.constructor.name;
    if (firstType !== secondType) {
        return false;
    }

    if (firstType === 'Array') {
        if (first.length !== second.length) {
            return false;
        }

        let equal = true;
        for (let i = 0; i < first.length; i++) {
            if (!isEqual(first[i], second[i])) {
                equal = false;
                break;
            }
        }

        return equal;
    }

    if (firstType === 'Object') {
        let equal = true;
        const fKeys = Object.keys(first);
        const sKeys = Object.keys(second);
        if (fKeys.length !== sKeys.length) {
            return false;
        }

        for (let i = 0; i < fKeys.length; i++) {
            if (first[fKeys[i]] && second[fKeys[i]]) {
                if (first[fKeys[i]] === second[fKeys[i]]) {
                    continue; // eslint-disable-line
                }

                if (
                    first[fKeys[i]] &&
                    (first[fKeys[i]].constructor.name === 'Array' ||
                        first[fKeys[i]].constructor.name === 'Object')
                ) {
                    equal = isEqual(first[fKeys[i]], second[fKeys[i]]);
                    if (!equal) {
                        break;
                    }
                } else if (first[fKeys[i]] !== second[fKeys[i]]) {
                    equal = false;
                    break;
                }
            } else if (
                (first[fKeys[i]] && !second[fKeys[i]]) ||
                (!first[fKeys[i]] && second[fKeys[i]])
            ) {
                equal = false;
                break;
            }
        }

        return equal;
    }

    return first === second;
};

// Replacement for lodash.camelCase
// https://stackoverflow.com/questions/2970525/converting-a-string-with-spaces-into-camel-case
export const camelCase = (str = '') => {
    return str
        .replace(/(?:^\w|[A-Z]|\b\w)/g, function (word, index) {
            return index === 0 ? word.toLowerCase() : word.toUpperCase();
        })
        .replace(/\s+/g, '');
};
