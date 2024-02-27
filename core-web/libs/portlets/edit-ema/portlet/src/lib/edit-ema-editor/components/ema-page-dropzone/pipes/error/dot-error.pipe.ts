import { Pipe, PipeTransform } from '@angular/core';

import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { Container, EmaDragItem } from '../../types';

interface DotErrorPipeResponse {
    message: string;
    args: string[];
}

@Pipe({
    name: 'dotError',
    standalone: true
})
export class DotErrorPipe implements PipeTransform {
    transform(
        { payload, contentlets }: Container,
        item: EmaDragItem | undefined
    ): DotErrorPipeResponse {
        const { container = {} } =
            typeof payload === 'string' ? JSON.parse(payload) : payload || {};

        const { acceptTypes = '', maxContentlets } = container;

        const contentletsLength = contentlets.length;

        if (!this.isValidContentType(acceptTypes, item)) {
            return {
                message: 'edit.ema.page.dropzone.invalid.contentlet.type',
                args: [item.contentType]
            };
        }

        if (!this.contentCanFitInContainer(container.maxContentlets, contentletsLength)) {
            const message =
                maxContentlets === 1
                    ? 'edit.ema.page.dropzone.one.max.contentlet'
                    : 'edit.ema.page.dropzone.max.contentlets';

            return {
                message,
                args: [maxContentlets.toString()]
            };
        }

        return {
            message: '',
            args: []
        };
    }

    private isValidContentType(acceptTypes: string, item: EmaDragItem | undefined): boolean {
        if (!item) {
            return false;
        }

        if (item.baseType === DotCMSBaseTypesContentTypes.WIDGET) {
            return true;
        }

        const acceptTypesArr = acceptTypes.split(',');

        return acceptTypesArr.includes(item.contentType);
    }

    private contentCanFitInContainer(maxContentlets: number, contentletsLength: number): boolean {
        return contentletsLength < maxContentlets;
    }
}
