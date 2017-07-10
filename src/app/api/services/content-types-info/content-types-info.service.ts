import { Injectable } from '@angular/core';

/**
 * Provide data for the content types items
 * @ContentTypesInfoService
 * @class CrudService
 */
@Injectable()
export class ContentTypesInfoService {
    private contentTypeInfoCollection = [
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            icon: 'fa-newspaper-o',
            label: 'content',
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            icon: 'fa-cog',
            label: 'widget',
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFileAssetContentType',
            icon: 'fa-file-o',
            label: 'fileasset',
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutablePageContentType',
            icon: 'fa-file-text-o',
            label: 'page',
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutablePersonaContentType',
            icon: 'fa-user',
            label: 'persona',
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFormContentType',
            icon: 'fa-list',
            label: 'form'
        }
    ];

    /**
     * Return an icon class base on the content type name
     *
     * @param {string} type
     * @returns {string}
     *
     * @memberof ContentTypesInfoService
     */
    public getIcon(type: string): string {
        return this.getItem(type, 'icon');
    }

    /**
     * Return an icon class property base on the content type name
     *
     * @param {string} type
     * @returns {string}
     *
     * @memberof ContentTypesInfoService
     */
    public getClazz(type: string): string {
        return this.getItem(type, 'clazz');
    }

    /**
     * Return the label property base on the content type name
     *
     * @param {string} type
     * @returns {string}
     *
     * @memberof ContentTypesInfoService
     */
    public getLabel(type: string): string {
        return this.getItem(type, 'label');
    }

    private getItem(type: string, prop: string): string {
        let result: any;

        if (type) {
            type = this.getTypeName(type);

            for (let i = 0; i < this.contentTypeInfoCollection.length; i++) {
                let item = this.contentTypeInfoCollection[i];
                if (item.clazz.toLocaleLowerCase() === type.toLocaleLowerCase() || item.label.toLocaleLowerCase() === type.toLocaleLowerCase()) {
                    result = item[prop];
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Some endpoints have different name/labels for the same content types
     * @param type
     */
    private getTypeName(type: string): string {
        if (type === 'HTMLPAGE' || type === 'htmlpage') {
            type = 'PAGE';
        }
        if (type === 'FILE' || type === 'file' || type === 'File') {
            type = 'FILEASSET';
        }

        return type;
    }

}