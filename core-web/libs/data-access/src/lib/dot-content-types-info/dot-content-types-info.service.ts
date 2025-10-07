import { Injectable } from '@angular/core';

import { DotCMSClazz } from '@dotcms/dotcms-models';

/**
 * Provide data for the content types items
 * @DotContentTypesInfoService
 * @class CrudService
 */
@Injectable()
export class DotContentTypesInfoService {
    private contentTypeInfoCollection = [
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            icon: 'event_note',
            label: 'content'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            icon: 'settings',
            label: 'widget'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFileAssetContentType',
            icon: 'insert_drive_file',
            label: 'fileasset'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableDotAssetContentType',
            icon: 'file_copy',
            label: 'dotasset'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutablePageContentType',
            icon: 'description',
            label: 'htmlpage'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutablePersonaContentType',
            icon: 'person',
            label: 'persona'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFormContentType',
            icon: 'format_list_bulleted',
            label: 'form'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType',
            icon: 'format_strikethrough',
            label: 'vanity_url'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableKeyValueContentType',
            icon: 'public',
            label: 'key_value'
        },
        /*
            PrimeNG used to use Font Awesome icons, now they are releasing with their own
            icon package "PrimeIcons" but they don't have many icons yet, so we keep
            using FA icons until we have a better icon offer from them.
        */
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            icon: 'fa fa-newspaper-o',
            label: 'content_old'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            icon: 'fa fa-cog',
            label: 'widget_old'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFileAssetContentType',
            icon: 'fa fa-file-o',
            label: 'fileasset_old'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableDotAssetContentType',
            icon: 'fa fa-files-o',
            label: 'dotasset_old'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutablePageContentType',
            icon: 'fa fa-file-text-o',
            label: 'htmlpage_old'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutablePersonaContentType',
            icon: 'fa fa-user',
            label: 'persona_old'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFormContentType',
            icon: 'fa fa-list',
            label: 'form_old'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType',
            icon: 'fa fa-map-signs',
            label: 'vanity_url_old'
        },
        {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableKeyValueContentType',
            icon: 'fa fa-globe',
            label: 'key_value_old'
        }
        // TODO: Remove this when set Material Design icons on NgPrime native components - END
    ];

    /**
     * Return an icon class base on the content type name
     *
     * @param string type
     * @returns string
     *
     * @memberof ContentTypesInfoService
     */
    getIcon(type: string): string {
        return this.getItem(type, 'icon');
    }

    /**
     * Return an icon class property base on the content type name
     *
     * @param string type
     * @returns string
     * @memberof ContentTypesInfoService
     */
    getClazz(type: string): DotCMSClazz {
        return this.getItem(type, 'clazz') as DotCMSClazz;
    }

    /**
     * Return the label property base on the content type name
     *
     * @param string type
     * @returns string
     * @memberof ContentTypesInfoService
     */
    getLabel(type: string): string {
        return this.getItem(type, 'label');
    }

    // tslint:disable-next-line:cyclomatic-complexity
    private getItem(type: string, prop: string): string {
        let result: string;
        // TODO: Remove this when set Material Design icons on NgPrime native components - BEGIN
        let oldValue = false;
        if (type.indexOf('_old') > 0) {
            oldValue = true;
            type = type.replace('_old', '');
        }
        // TODO: Remove this when set Material Design icons on NgPrime native components - END

        if (type) {
            type = this.getTypeName(type);

            for (let i = 0; i < this.contentTypeInfoCollection.length; i++) {
                const item = this.contentTypeInfoCollection[i];
                if (
                    item.clazz.toLocaleLowerCase() === type.toLocaleLowerCase() ||
                    item.label.toLocaleLowerCase() ===
                        (oldValue ? `${type}_old` : type).toLocaleLowerCase()
                ) {
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
        if (type === 'htmlpage') {
            type = 'HTMLPAGE';
        }

        if (type === 'FILE' || type === 'file' || type === 'File') {
            type = 'FILEASSET';
        }

        return type;
    }
}
