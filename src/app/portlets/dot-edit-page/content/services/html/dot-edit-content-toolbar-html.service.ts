import { Injectable } from '@angular/core';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';

interface DotEditPopupMenuItem {
    label: string;
    disabled?: boolean;
    dataset: {
        [propName: string]: string;
    };
}

interface DotEditPopupButton {
    label: string;
    class?: string;
}

interface DotEditPopupMenu {
    button: DotEditPopupButton;
    items?: DotEditPopupMenuItem[];
}

/**
 * Service to generate the markup related with the Toolbars and sub-menu for containers.
 */
@Injectable()
export class DotEditContentToolbarHtmlService {
    private dragLabel: string;
    private removeLabel: string;
    private editLabel: string;

    constructor(private dotMessageService: DotMessageService, private dotDOMHtmlUtilService: DotDOMHtmlUtilService) {}

    addContainerToolbar(doc: any): Promise<any> {
        return new Promise((resolve, reject) => {
            this.dotMessageService
                .getMessages([
                    'editpage.content.container.action.add',
                    'editpage.content.container.menu.content',
                    'editpage.content.container.menu.widget',
                    'editpage.content.container.menu.form'
                ])
                .subscribe(
                    (messages: string[]) => {
                        if (messages.length === 0) {
                            reject();
                        }

                        try {
                            const containers = Array.from(doc.querySelectorAll('div[data-dot-object="container"]'));
                            containers.forEach((container: HTMLElement) => {
                                const containerToolbar = document.createElement('div');
                                containerToolbar.classList.add('dotedit-container__toolbar');

                                if (!container.dataset.dotCanAdd.length) {
                                    container.classList.add('disabled');
                                }

                                containerToolbar.innerHTML = this.getContainerToolbarHtml(container);
                                container.parentNode.insertBefore(containerToolbar, container);
                            });
                            resolve();
                        } catch (error) {
                            reject(error);
                        }
                    },
                    (error) => {
                        reject(error);
                    }
                );
        });
    }

    addContentletMarkup(doc: any): Promise<any> {
        return new Promise((resolve, reject) => {
            this.dotMessageService
                .getMessages([
                    'editpage.content.container.action.edit.vtl',
                    'editpage.content.contentlet.menu.drag',
                    'editpage.content.contentlet.menu.edit',
                    'editpage.content.contentlet.menu.remove'
                ])
                .subscribe(
                    (messages: any) => {
                        if (!Object.keys(messages).length) {
                            reject();
                        }

                        try {
                            const contentlets = Array.from(doc.querySelectorAll('div[data-dot-object="contentlet"]'));
                            contentlets.forEach((contentlet: HTMLElement) => {
                                const contentletToolbar = document.createElement('div');
                                contentletToolbar.classList.add('dotedit-contentlet__toolbar');

                                (this.dragLabel = messages['editpage.content.contentlet.menu.drag']),
                                    (this.editLabel = messages['editpage.content.contentlet.menu.edit']),
                                    (this.removeLabel = messages['editpage.content.contentlet.menu.remove']);

                                const vtls = Array.from(contentlet.querySelectorAll('div[data-dot-object="vtl-file"]'));

                                if (vtls.length) {
                                    contentletToolbar.innerHTML += this.getEditVtlButtons(vtls);
                                }

                                contentletToolbar.innerHTML += this.getContentButton(
                                    contentlet.dataset.dotIdentifier,
                                    contentlet.dataset.dotInode,
                                    contentlet.dataset.dotCanEdit === 'true'
                                );

                                const contentletContent = document.createElement('div');
                                contentletContent.classList.add('dotedit-contentlet__content');
                                contentletContent.innerHTML = contentlet.innerHTML;
                                contentlet.innerHTML = '';

                                contentlet.insertAdjacentElement('afterbegin', contentletContent);
                                contentlet.insertAdjacentElement('afterbegin', contentletToolbar);
                            });
                            resolve();
                        } catch (error) {
                            reject(error);
                        }
                    },
                    (error) => {
                        reject(error);
                    }
                );
        });
    }

    getContentButton(identifier: string, inode: string, canEdit?: boolean): string {
        const dataset = {
            'dot-identifier': identifier,
            'dot-inode': inode
        };

        let editButtonClass = 'dotedit-contentlet__edit';
        editButtonClass += !canEdit ? ' dotedit-contentlet__disabled' : '';

        return `${this.dotDOMHtmlUtilService.getButtomHTML(this.dragLabel, 'dotedit-contentlet__drag', dataset)}
            ${this.dotDOMHtmlUtilService.getButtomHTML(this.editLabel, editButtonClass, dataset)}
            ${this.dotDOMHtmlUtilService.getButtomHTML(this.removeLabel, 'dotedit-contentlet__remove', dataset)}`;
    }

    private getEditVtlButtons(vtls: any[]): string {
        return this.getDotEditPopupMenuHtml({
            button: {
                label: this.dotMessageService.get('editpage.content.container.action.edit.vtl'),
                class: 'dotedit-contentlet__code'
            },
            items: vtls.map((vtl: HTMLElement) => {
                return {
                    disabled: vtl.dataset.dotCanEdit === 'false',
                    label: vtl.dataset.dotUrl.split('/').slice(-1)[0],
                    dataset: {
                        action: 'code',
                        inode: vtl.dataset.dotInode
                    }
                };
            })
        });
    }

    private getContainerToolbarHtml(container: HTMLElement): string {
        return this.getDotEditPopupMenuHtml({
            button: {
                label: `${this.dotMessageService.get('editpage.content.container.action.add')}`,
                class: 'dotedit-container__add'
            },
            items: container.dataset.dotCanAdd
                .split(',')
                .filter((item: string) => item.length)
                .map((item: string) => {
                    item = item.toLowerCase();

                    return {
                        label: this.dotMessageService.get(`editpage.content.container.menu.${item}`),
                        dataset: {
                            action: 'add',
                            add: item,
                            identifier: container.dataset.dotIdentifier,
                            uuid: container.dataset.dotUuid
                        }
                    };
                })
        });
    }

    private getDotEditPopupMenuHtml(menu: DotEditPopupMenu): string {
        const isMenuItems = menu.items.length > 0;

        let result = '<div class="dotedit-menu">';

        result += this.getDotEditPopupMenuButton(menu.button, !isMenuItems);

        if (isMenuItems) {
            result += this.getDotEditPopupMenuList(menu.items);
        }

        result += '</div>';

        return result;
    }

    private getDotEditPopupMenuButton(button: DotEditPopupButton, disabled = false): string {
        return `
            <button
                type="button"
                class="dotedit-menu__button ${button.class ? button.class : ''}"
                aria-label="${button.label}"
                ${disabled ? 'disabled' : ''}>
            </button>
        `;
    }

    private getDotEditPopupMenuList(items: DotEditPopupMenuItem[]): string {
        return `
            <ul class="dotedit-menu__list" >
                ${items
                    .map((item: DotEditPopupMenuItem) => {
                        return `
                            <li class="dotedit-menu__item ${item.disabled ? 'dotedit-menu__item--disabled' : ''}">
                                <a href="#" ${this.getDotEditPopupMenuItemDataSet(item.dataset)} role="button">${item.label}</a>
                            </li>
                        `;
                    })
                    .join('')}
            </ul>
        `;
    }

    private getDotEditPopupMenuItemDataSet(datasets: { [propName: string]: string }): string {
        return Object.keys(datasets)
            .map((key) => `data-dot-${key}="${datasets[key]}"`)
            .join(' ');
    }
}
