const escapeRegExp = (string: string) => {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};

export const CountOccurrences = (str: string, searchStr: string) => {
    const escapedSearchStr = escapeRegExp(searchStr);

    return (str.match(new RegExp(escapedSearchStr, 'gi')) || []).length;
};
