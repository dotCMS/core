import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { DotCMSContentlet, DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotContentTypeService } from '@services/dot-content-type';
import { Observable } from 'rxjs';
import { DotContentCompareEvent } from '@components/dot-content-compare/dot-content-compare.component';
import { map, switchMap, take } from 'rxjs/operators';
import { DotContentletService } from '@services/dot-contentlet/dot-contentlet.service';
import { DotFormatDateService } from '@services/dot-format-date-service';

export interface DotContentCompareTableData {
    working: DotCMSContentlet;
    compare: DotCMSContentlet;
    versions: DotCMSContentlet[];
    fields: DotCMSContentTypeField[];
}

export interface DotContentCompareState {
    data: DotContentCompareTableData;
    showDiff: boolean;
}

enum DateFormat {
    Date = 'MM/dd/yyyy',
    Time = 'hh:mm aa',
    'Date-and-Time' = 'MM/dd/yyyy - hh:mm aa'
}

export enum FieldWhiteList {
    Text = 'Text',
    Textarea = 'Textarea',
    Checkbox = 'Checkbox',
    'Constant-Field' = 'Constant-Field',
    'Key-Value' = 'Key-Value',
    Radio = 'Radio',
    Select = 'Select',
    'Multi-Select' = 'Multi-Select',
    Tag = 'Tag',
    'Custom-Field' = 'Custom-Field',
    'Hidden-Field' = 'Hidden-Field',
    Image = 'image',
    File = 'File',
    Binary = 'Binary',
    Category = 'Category',
    Date = 'Date',
    'Date-and-Time' = 'Date-and-Time',
    Time = 'Time',
    'WYSIWYG' = 'WYSIWYG',
    'Host-Folder' = 'Host-Folder'
}

@Injectable()
export class DotContentCompareStore extends ComponentStore<DotContentCompareState> {
    systemTime;

    constructor(
        private dotContentTypeService: DotContentTypeService,
        private dotContentletService: DotContentletService,
        private dotFormatDateService: DotFormatDateService
    ) {
        super({
            data: null,
            showDiff: true
        });
    }

    readonly vm$ = this.state$;

    // UPDATERS
    private readonly updateData = this.updater((state, data: DotContentCompareTableData) => {
        return { ...state, data };
    });

    readonly updateCompare = this.updater((state, compare: DotCMSContentlet) => {
        return { ...state, data: { ...state.data, compare } };
    });

    readonly updateShowDiff = this.updater((state, showDiff: boolean) => {
        return { ...state, showDiff };
    });

    private filterFields(contentType: DotCMSContentType): DotCMSContentTypeField[] {
        return contentType.fields.filter((field) => FieldWhiteList[field.fieldType] != undefined);
    }

    private getContentByInode(inode: string, contents: DotCMSContentlet[]): DotCMSContentlet {
        return contents.find((content) => content.inode === inode);
    }

    private getWorkingVersion(contents: DotCMSContentlet[]): DotCMSContentlet {
        return contents.find((content) => content.working === true);
    }

    private getFieldFormattedValue(value: any, fieldType: string): string {
        if (value) {
            switch (fieldType) {
                case FieldWhiteList.Category: {
                    return value
                        .map((obj) => {
                            return Object.values(obj)[0];
                        })
                        .join(',');
                }
                case FieldWhiteList['Key-Value']: {
                    let string = '';
                    Object.entries(value).forEach(([key, value]) => {
                        string += `${key}: ${value} <br/>`;
                    });
                    return string;
                }
                default: {
                    //is a Date related field.
                    return this.dotFormatDateService.formatTZ(
                        new Date(value as string),
                        DateFormat[fieldType]
                    );
                }
            }
        }
        return value;
    }

    private convertContentDates(
        contents: DotCMSContentlet[],
        fields: DotCMSContentTypeField[]
    ): DotCMSContentlet[] {
        const types = ['Date', 'Time', 'Date-and-Time', 'Category', 'Key-Value'];
        let fieldNeedFormat: DotCMSContentTypeField[] = [];
        fields.forEach((field) => {
            if (types.includes(field.fieldType)) {
                fieldNeedFormat.push(field);
            }
        });
        contents.forEach((content) => {
            fieldNeedFormat.forEach((field) => {
                content[field.variable] = this.getFieldFormattedValue(
                    content[field.variable],
                    field.fieldType
                );
            });
            content.modDate = this.dotFormatDateService.formatTZ(
                new Date(content.modDate),
                'MM/dd/yyyy - hh:mm aa'
            );
        });

        return contents;
    }

    //Effects

    readonly loadData = this.effect((data$: Observable<DotContentCompareEvent>) => {
        return data$.pipe(
            map((data) => {
                this.dotContentletService
                    .getContentletVersions(data.identifier, data.language)
                    .pipe(
                        take(1),
                        switchMap((contents: DotCMSContentlet[]) => {
                            return this.dotContentTypeService
                                .getContentType(contents[0].contentType)
                                .pipe(
                                    take(1),
                                    map((contentType: DotCMSContentType) => {
                                        return { contentType, contents };
                                    })
                                );
                        })
                    )
                    .subscribe(
                        (value: {
                            contentType: DotCMSContentType;
                            contents: DotCMSContentlet[];
                        }) => {
                            const fields = this.filterFields(value.contentType);
                            const formattedContents = this.convertContentDates(
                                value.contents,
                                fields
                            );
                            this.updateData({
                                working: this.getWorkingVersion(formattedContents),
                                compare: this.getContentByInode(data.inode, formattedContents),
                                versions: formattedContents.filter(
                                    (content) => content.working === false
                                ),
                                fields: fields
                            });
                            this.updateShowDiff(true);
                        }
                    );
            })
        );
    });
}
