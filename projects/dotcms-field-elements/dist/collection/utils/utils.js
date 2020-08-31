export function getClassNames(status, isValid, required) {
    return {
        'dot-valid': isValid,
        'dot-invalid': !isValid,
        'dot-pristine': status.dotPristine,
        'dot-dirty': !status.dotPristine,
        'dot-touched': status.dotTouched,
        'dot-untouched': !status.dotTouched,
        'dot-required': required
    };
}
export function isStringType(val) {
    return typeof val === 'string' && !!val;
}
export function getDotOptionsFromFieldValue(rawString) {
    if (!isStringType(rawString)) {
        return [];
    }
    rawString = rawString.replace(/(?:\\[rn]|[\r\n]+)+/g, ',');
    const items = isKeyPipeValueFormatValid(rawString)
        ? rawString
            .split(',')
            .filter((item) => !!item.length)
            .map((item) => {
            const [label, value] = item.split('|');
            return { label, value };
        })
        : [];
    return items;
}
export function getErrorClass(valid) {
    return valid ? undefined : 'dot-field__error';
}
export function getHintId(name) {
    const value = slugify(name);
    return value ? `hint-${value}` : undefined;
}
export function getId(name) {
    const value = slugify(name);
    return name ? `dot-${slugify(value)}` : undefined;
}
export function getLabelId(name) {
    const value = slugify(name);
    return value ? `label-${value}` : undefined;
}
export function getOriginalStatus(isValid) {
    return {
        dotValid: typeof isValid === 'undefined' ? true : isValid,
        dotTouched: false,
        dotPristine: true
    };
}
export function getStringFromDotKeyArray(values) {
    return values.map((item) => `${item.key}|${item.value}`).join(',');
}
export function updateStatus(state, change) {
    return Object.assign({}, state, change);
}
export function getTagError(show, message) {
    return show && isStringType(message) ? (h("span", { class: "dot-field__error-message" }, message)) : null;
}
export function getTagHint(hint) {
    return isStringType(hint) ? (h("span", { class: "dot-field__hint", id: getHintId(hint) }, hint)) : null;
}
export function isValidURL(url) {
    try {
        return !!new URL(url);
    }
    catch (e) {
        return false;
    }
}
export function isFileAllowed(fileName, allowedExtensions) {
    let allowedExtensionsArray = allowedExtensions.split(',');
    allowedExtensionsArray = allowedExtensionsArray.map((item) => item.trim());
    const extension = fileName ? fileName.substring(fileName.indexOf('.'), fileName.length) : '';
    return allowAnyFile(allowedExtensionsArray) || allowedExtensionsArray.includes(extension);
}
function allowAnyFile(allowedExtensions) {
    return allowedExtensions[0] === '' || allowedExtensions.includes('*');
}
function slugify(text) {
    return text
        ? text
            .toString()
            .toLowerCase()
            .replace(/\s+/g, '-')
            .replace(/[^\w\-]+/g, '')
            .replace(/\-\-+/g, '-')
            .replace(/^-+/, '')
            .replace(/-+$/, '')
        : null;
}
function isKeyPipeValueFormatValid(rawString) {
    const regex = /([^|,]*)\|([^|,]*)/;
    const items = rawString.split(',');
    let valid = true;
    for (let i = 0, total = items.length; i < total; i++) {
        if (!regex.test(items[i])) {
            valid = false;
            break;
        }
    }
    return valid;
}
