import { Injectable } from '@angular/core';

@Injectable()
export class DotBinaryFieldValidatorService {
    private _maxFileSize: number;
    private _accept: string[] = [];
    private _acceptSanitized: string[] = [];

    get accept(): string[] {
        return this._accept;
    }

    get maxFileSize(): number {
        return this._maxFileSize;
    }

    setMaxFileSize(maxFileSize: number) {
        this._maxFileSize = maxFileSize;
    }

    setAccept(accept: string[]) {
        this._accept = accept;
        this._acceptSanitized = accept
            ?.filter((value) => value !== '*/*')
            .map((type) => {
                // Remove the wildcard character
                return type.toLowerCase().replace(/\*/g, '');
            });
    }

    isValidType({ extension, mimeType }): boolean {
        if (this._acceptSanitized?.length === 0) {
            return true;
        }

        const sanitizedExtension = extension?.replace('.', '');

        return this._acceptSanitized.some(
            (type) => mimeType?.includes(type) || type?.includes(`.${sanitizedExtension}`)
        );
    }

    isValidSize(size: number): boolean {
        if (!this._maxFileSize) {
            return true;
        }

        return size <= this._maxFileSize;
    }
}
