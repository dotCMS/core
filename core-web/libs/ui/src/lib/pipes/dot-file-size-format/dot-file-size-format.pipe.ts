import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'dotFileSizeFormat'
})
export class DotFileSizeFormatPipe implements PipeTransform {
    transform(bytes: number): string {
        if (!bytes) return '0 Bytes';

        const kylobyte = 1024;
        const units = ['Bytes', 'KB', 'MB', 'GB'];

        // Calculate the index of the appropriate unit in the units array
        // This is done by taking the base-1024 logarithm of the file size
        const potentialIndex = Math.floor(Math.log(bytes) / Math.log(kylobyte));

        const index = Math.min(potentialIndex, units.length - 1);
        const unit = units[index]; // Select the appropriate unit from the units array

        // Calculate the file size in the appropriate unit
        // This is done by dividing the file size in bytes by the appropriate power of 1024
        const value = bytes / Math.pow(kylobyte, index);

        const size = value % 1 !== 0 ? value.toFixed(2) : value;

        return `${size} ${unit}`;
    }
}
