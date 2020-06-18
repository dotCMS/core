import { Pipe, PipeTransform } from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

@Pipe({
    name: 'dm'
})
export class DotMessagePipe implements PipeTransform {
    constructor(private dotMessageService: DotMessageService) {}

    transform(value: string, args?: string[]): string {
        return value ? this.dotMessageService.get(value, ...args) : '';
    }
}
