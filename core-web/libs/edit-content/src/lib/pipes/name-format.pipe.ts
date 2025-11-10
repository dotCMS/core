import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formats names according to the following rules:
 * - For two names: first name initial + last name (e.g., "John Doe" -> "J. Doe")
 * - For three or more names: takes first name and first surname (third position)
 * - If the result exceeds maxLength (default 10) characters, returns initials only
 *
 * Examples:
 * - "John Doe" -> "J. Doe"
 * - "John William Smith" -> "J. Smith" (or "J. S." if too long)
 * - "John William Rodriguez Smith" -> "J. R." (due to length limit)
 *
 * Usage:
 * {{ name | dotNameFormat }}         // uses default 10 characters limit
 * {{ name | dotNameFormat:15 }}      // uses 15 characters limit
 */
@Pipe({
    name: 'dotNameFormat',
    pure: true
})
export class DotNameFormatPipe implements PipeTransform {
    private readonly DEFAULT_MAX_LENGTH = 10;

    transform(
        fullName: string | null | undefined,
        maxLength: number = this.DEFAULT_MAX_LENGTH
    ): string {
        if (!fullName) return '';

        // Normalize multiple spaces and trim
        const normalizedName = fullName.trim().replace(/\s+/g, ' ');

        const parts = normalizedName.split(' ');
        if (parts.length < 2) return normalizedName;

        const firstName = parts[0];

        // For three or more names, take first name and first surname (third position)
        if (parts.length >= 3) {
            const firstLastName = parts[2];
            const fullFormat = `${firstName.charAt(0)}. ${firstLastName}`;

            // If exceeds length limit, use initials only
            if (fullFormat.length > maxLength) {
                return `${firstName.charAt(0)}. ${firstLastName.charAt(0)}.`;
            }

            return fullFormat;
        }

        // For two names
        const fullFormat = `${firstName.charAt(0)}. ${parts[1]}`;
        if (fullFormat.length > maxLength) {
            return `${firstName.charAt(0)}. ${parts[1].charAt(0)}.`;
        }

        return fullFormat;
    }
}
