import { Injectable } from '@angular/core';

@Injectable()
export class DotBinaryFieldValidatorService {
    #maxFileSize: number;
    #accept: string[] = [];
    #acceptSanitized: string[] = [];

    get accept(): string[] {
        return this.#accept;
    }

    get maxFileSize(): number {
        return this.#maxFileSize;
    }

    setMaxFileSize(maxFileSize: number) {
        this.#maxFileSize = maxFileSize;
    }

    setAccept(accept: string[]) {
        this.#accept = accept;
        this.#acceptSanitized = accept
            ?.filter((value) => value !== '*/*')
            .map((type) => {
                // Remove the wildcard character
                return type.toLowerCase().replace(/\*/g, '');
            });
    }

    isValidType({ extension, mimeType }): boolean {
        if (this.#acceptSanitized?.length === 0) {
            return true;
        }

        const sanitizedExtension = extension?.replace('.', '');

        return this.#acceptSanitized.some(
            (type) => mimeType?.includes(type) || type?.includes(`.${sanitizedExtension}`)
        );
    }

    isValidSize(size: number): boolean {
        if (!this.#maxFileSize) {
            return true;
        }

        return size <= this.#maxFileSize;
    }
}
