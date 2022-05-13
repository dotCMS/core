import { Pipe, PipeTransform } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

@Pipe({
    name: 'dotTransformVersionLabel'
})
export class DotTransformVersionLabelPipe implements PipeTransform {
    constructor(
        private dotFormatDateService: DotFormatDateService,
        private dotMessageService: DotMessageService
    ) {}

    transform(item: DotCMSContentlet): string {
        const modDateClean = new Date(item.modDate.replace('- ', ''));
        const currentServerDate = new Date(
            this.dotFormatDateService.formatTZ(new Date(), 'MM/dd/yyyy hh:mm:ss aa')
        );
        const relativeDateStr = this.dotFormatDateService.getRelative(
            new Date(modDateClean).getTime().toString(),
            currentServerDate
        );

        const diffDays = this.dotFormatDateService.differenceInCalendarDays(
            currentServerDate,
            modDateClean
        );

        return this.getDateLabel(diffDays, item, relativeDateStr);
    }

    private getDateLabel(
        diffDays: number,
        item: DotCMSContentlet,
        relativeDateStr: string
    ): string {
        const dateLabel = diffDays > 6 ? item.modDate : relativeDateStr;
        return `${dateLabel} ${this.dotMessageService.get('by')} ${item.modUserName}`;
    }
}
