/**
 * What `uploadAssets` and `downloadAssets` share: the entries their manifests are made of, the
 * dotCMS path form both accept, and how a manifest is finished.
 */

import { ValidationError } from '../../../runtime';

export interface AssetManifestFile {
    path: string;
    bytes: number;
    identifier?: string;
}

export interface AssetManifestFailure {
    path: string;
    error: string;
}

export interface AssetManifestSkipped {
    path: string;
    reason: string;
}

export function normalizeDotCMSPath(input: string): { siteQualified?: string; path: string } {
    const value = input.trim().replace(/\/+$/, '');

    if (value.startsWith('//')) {
        const firstSlash = value.slice(2).indexOf('/');
        if (firstSlash < 0) {
            throw new ValidationError(`Site-qualified path "${input}" must include a path`);
        }

        return { siteQualified: value, path: value.slice(firstSlash + 2) };
    }

    if (!value.startsWith('/')) {
        throw new ValidationError(`dotCMS path "${input}" must start with "/" or "//host/"`);
    }

    return { path: value };
}

export function sumBytes(files: AssetManifestFile[]): number {
    return files.reduce((sum, file) => sum + file.bytes, 0);
}

export function sortManifest<
    T extends {
        files: AssetManifestFile[];
        failures: AssetManifestFailure[];
        skipped?: AssetManifestSkipped[];
        notLive?: AssetManifestFile[];
    }
>(manifest: T): T {
    const byPath = (a: { path: string }, b: { path: string }) => a.path.localeCompare(b.path);
    manifest.files.sort(byPath);
    manifest.failures.sort(byPath);
    manifest.skipped?.sort(byPath);
    manifest.notLive?.sort(byPath);
    return manifest;
}
