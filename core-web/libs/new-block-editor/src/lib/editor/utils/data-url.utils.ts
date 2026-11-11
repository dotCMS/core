/**
 * `data:<mime>[;param…][;base64],<payload>` — the three captures are the mime type, the
 * parameter list (with its leading `;`) and the payload.
 */
const DATA_URL_PATTERN = /^data:([^;,]+)((?:;[^;,]+)*),(.*)$/is;

/** Mime types whose conventional extension is not simply the subtype. */
const EXTENSION_BY_MIME: Record<string, string> = {
    'image/jpeg': 'jpg',
    'image/svg+xml': 'svg',
    'image/x-icon': 'ico',
    'image/vnd.microsoft.icon': 'ico'
};

/** True when the string is a `data:` URL (any media type). */
export function isDataUrl(value: string): boolean {
    return /^data:/i.test(value);
}

/** File extension for an image mime type: the subtype, except where convention differs. */
function extensionForMime(mime: string): string {
    return EXTENSION_BY_MIME[mime] ?? mime.slice(mime.indexOf('/') + 1).replace(/[^a-z0-9]/g, '');
}

/**
 * Decodes an image `data:` URL (base64 or percent-encoded) into a {@link File} that the upload
 * service can publish as a dotAsset. Pasted HTML and Markdown carry images this way, and the
 * editor must store an identifier-based image rather than the inline bytes (#38165).
 *
 * @param dataUrl The `data:image/…` URL to decode.
 * @param name File name to give the result; an extension matching the mime type is appended when
 * the name has none.
 * @returns The decoded file, or `null` when the URL is not an image data URL or cannot be decoded.
 */
export function dataUrlToFile(dataUrl: string, name: string): File | null {
    const match = DATA_URL_PATTERN.exec(dataUrl);
    if (!match) return null;

    const mime = match[1].trim().toLowerCase();
    if (!mime.startsWith('image/')) return null;

    const isBase64 = match[2]
        .split(';')
        .some((parameter) => parameter.trim().toLowerCase() === 'base64');
    const payload = match[3];

    let bytes: Uint8Array;
    try {
        if (isBase64) {
            const binary = atob(payload.replace(/\s/g, ''));
            bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
        } else {
            bytes = new TextEncoder().encode(decodeURIComponent(payload));
        }
    } catch {
        return null;
    }

    const fileName = /\.[a-z0-9]+$/i.test(name) ? name : `${name}.${extensionForMime(mime)}`;

    return new File([bytes.buffer as ArrayBuffer], fileName, { type: mime });
}
