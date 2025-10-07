import { ComponentStore } from '@ngrx/component-store';
import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, switchMap, take } from 'rxjs/operators';

import {
    DotContentletService,
    DotContentTypeService,
    DotFormatDateService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotContentCompareEvent
} from '@dotcms/dotcms-models';

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
    'Story-Block' = 'Story-Block',
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
    'Host-Folder' = 'Host-Folder',
    'JSON-Field' = 'JSON-Field'
}

@Injectable()
export class DotContentCompareStore extends ComponentStore<DotContentCompareState> {
    private dotContentTypeService = inject(DotContentTypeService);
    private dotContentletService = inject(DotContentletService);
    private dotFormatDateService = inject(DotFormatDateService);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);

    systemTime;
    readonly vm$ = this.state$;
    readonly updateCompare = this.updater((state, compare: DotCMSContentlet) => {
        return { ...state, data: { ...state.data, compare } };
    });
    readonly updateShowDiff = this.updater((state, showDiff: boolean) => {
        return { ...state, showDiff };
    });
    // UPDATERS
    private readonly updateData = this.updater((state, data: DotContentCompareTableData) => {
        return { ...state, data };
    });
    //Effects
    readonly loadData = this.effect((data$: Observable<DotContentCompareEvent>) => {
        return data$.pipe(
            map((data) => {
                this.dotContentletService
                    .getContentletVersions(data.identifier, data.language)
                    .pipe(
                        take(1),
                        catchError((err: HttpErrorResponse) => {
                            return this.httpErrorManagerService.handle(err);
                        }),
                        switchMap((contents: DotCMSContentlet[]) => {
                            console.log('contents', contents);

                            return this.dotContentTypeService
                                .getContentType(contents[0].contentType)
                                .pipe(
                                    take(1),
                                    map((contentType: DotCMSContentType) => {
                                        return { contentType, contents };
                                    })
                                );
                        }),
                        // If the requested item is not found in the list of historical content versions, attempt to add it.
                        switchMap(
                            (value: {
                                contentType: DotCMSContentType;
                                contents: DotCMSContentlet[];
                            }) => {
                                return !value.contents.some(
                                    (content) => content.inode === data.inode
                                )
                                    ? this.dotContentletService
                                          .getContentletByInode(data.inode)
                                          .pipe(
                                              map((response: DotCMSContentlet) => {
                                                  return {
                                                      ...value,
                                                      contents: [...value.contents, response]
                                                  };
                                              }),
                                              catchError((err: HttpErrorResponse) => {
                                                  return this.httpErrorManagerService.handle(err);
                                              })
                                          )
                                    : of(value);
                            }
                        )
                    )
                    .subscribe(
                        (value: {
                            contentType: DotCMSContentType;
                            contents: DotCMSContentlet[];
                        }) => {
                            if (!value || !value.contents || !value.contentType) {
                                return;
                            }

                            const fields = this.filterFields(value.contentType);
                            const formattedContents = this.formatSpecificTypesFields(
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

    constructor() {
        super({
            data: null,
            showDiff: true
        });
    }

    private filterFields(contentType: DotCMSContentType): DotCMSContentTypeField[] {
        return contentType.fields.filter((field) => FieldWhiteList[field.fieldType] != undefined);
    }

    private getContentByInode(inode: string, contents: DotCMSContentlet[]): DotCMSContentlet {
        return contents.find((content) => content.inode === inode);
    }

    private getWorkingVersion(contents: DotCMSContentlet[]): DotCMSContentlet {
        return contents.find((content) => content.working === true);
    }

    private getFieldFormattedValue(
        value: string | { [key: string]: string } | [],
        fieldType: string
    ): string {
        if (value) {
            switch (fieldType) {
                case FieldWhiteList.Category: {
                    return (value as [])
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

        return value as string;
    }

    private formatSpecificTypesFields(
        contents: DotCMSContentlet[],
        fields: DotCMSContentTypeField[]
    ): DotCMSContentlet[] {
        const types = ['Date', 'Time', 'Date-and-Time', 'Category', 'Key-Value'];
        const fieldNeedFormat: DotCMSContentTypeField[] = [];
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
}
