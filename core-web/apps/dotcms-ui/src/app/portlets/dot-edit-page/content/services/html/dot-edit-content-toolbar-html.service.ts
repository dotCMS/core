import { Injectable, inject } from '@angular/core';

import { take } from 'rxjs/operators';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';

import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';

interface DotEditPopupMenuItem {
    label: string;
    disabled?: boolean;
    tooltip?: string;
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

enum ValidationError {
    EnterpriseLicenseError,
    MaxContentletsLimitReachedError
}

/**
 * Service to generate the markup related with the Toolbars and sub-menu for containers.
 */
@Injectable()
export class DotEditContentToolbarHtmlService {
    private dotMessageService = inject(DotMessageService);
    private dotDOMHtmlUtilService = inject(DotDOMHtmlUtilService);
    private dotLicenseService = inject(DotLicenseService);

    isEnterpriseLicense: boolean;

    /**
     * Add custom HTML buttons to the containers div
     *
     * @param Document doc
     * @memberof DotEditContentToolbarHtmlService
     */
    addContainerToolbar(doc: Document): void {
        this.dotLicenseService
            .isEnterprise()
            .pipe(take(1))
            .subscribe((isEnterpriseLicense: boolean) => {
                this.isEnterpriseLicense = isEnterpriseLicense;

                const containers = Array.from(
                    doc.querySelectorAll('[data-dot-object="container"]')
                );
                containers.forEach((container: HTMLElement) => {
                    this.createContainerToolbar(container);
                });
            });
    }

    /**
     * Updates DOM with updated version of a specific Container toolbar
     *
     * @param HTMLElement container
     * @memberof DotEditContentToolbarHtmlService
     */
    updateContainerToolbar(container: HTMLElement): void {
        if (container.parentNode) {
            const toolbar = container.parentNode.querySelector(
                `[data-dot-container-inode="${container.dataset['dotInode']}"]`
            );
            container.parentNode.removeChild(toolbar);
            this.createContainerToolbar(container);
        }
    }

    /**
     * Bind event to the document to add the contentlet toolbar on contentlet element mouseover
     *
     * @param {Document} doc
     * @memberof DotEditContentToolbarHtmlService
     */
    bindContentletEvents(doc: Document): void {
        doc.addEventListener('mouseover', (e) => {
            const contentlet: HTMLElement = (e.target as Element).closest(
                '[data-dot-object="contentlet"]:not([data-dot-toolbar="true"]'
            );

            if (contentlet) {
                contentlet.setAttribute('data-dot-toolbar', 'true');
                this.addToolbarToContentlet(contentlet);
            }
        });
    }

    /**
     * Return the HTML of the buttons for the contentlets
     *
     * @param {{ [key: string]: string }} contentletDataset
     * @returns {string}
     * @memberof DotEditContentToolbarHtmlService
     */
    getContentButton(contentletDataset: { [key: string]: string }): string {
        const identifier: string = contentletDataset.dotIdentifier;
        const inode: string = contentletDataset.dotInode;
        const canEdit: boolean = contentletDataset.dotCanEdit === 'true';
        const isForm: boolean = contentletDataset.dotBasetype === 'FORM';

        const dataset = {
            'dot-identifier': identifier,
            'dot-inode': inode
        };

        let editButtonClass = 'dotedit-contentlet__edit';
        editButtonClass += !canEdit || isForm ? ' dotedit-contentlet__disabled' : '';

        return `
            ${this.dotDOMHtmlUtilService.getButtomHTML(
                this.dotMessageService.get('editpage.content.contentlet.menu.drag'),
                'dotedit-contentlet__drag',
                {
                    ...dataset,
                    'dot-object': 'drag-content'
                }
            )}
            ${this.dotDOMHtmlUtilService.getButtomHTML(
                this.dotMessageService.get('editpage.content.contentlet.menu.edit'),
                editButtonClass,
                {
                    ...dataset,
                    'dot-object': 'edit-content'
                }
            )}
            ${this.dotDOMHtmlUtilService.getButtomHTML(
                this.dotMessageService.get('editpage.content.contentlet.menu.remove'),
                'dotedit-contentlet__remove',
                {
                    ...dataset,
                    'dot-object': 'remove-content'
                }
            )}
        `;
    }

    /**
     * Returns the html for the edit vlt buttons
     *
     * @param {HTMLElement[]} vtls
     * @returns {string}
     * @memberof DotEditContentToolbarHtmlService
     */
    getEditVtlButtons(vtls: HTMLElement[]): string {
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

    private addToolbarToContentlet(contentlet: HTMLElement) {
        const contentletToolbar = document.createElement('div');
        contentletToolbar.classList.add('dotedit-contentlet__toolbar');

        const vtls: HTMLElement[] = Array.from(
            contentlet.querySelectorAll('[data-dot-object="vtl-file"]')
        );

        if (vtls.length) {
            contentletToolbar.innerHTML += this.getEditVtlButtons(vtls);
        }

        contentletToolbar.innerHTML += this.getContentButton(contentlet.dataset);

        contentlet.insertAdjacentElement('afterbegin', contentletToolbar);
    }

    private createContainerToolbar(container: HTMLElement) {
        const containerToolbar = document.createElement('div');
        containerToolbar.classList.add('dotedit-container__toolbar');
        containerToolbar.setAttribute('data-dot-container-inode', container.dataset['dotInode']);

        if (!container.dataset.dotCanAdd.length) {
            container.classList.add('disabled');
        }

        containerToolbar.innerHTML = this.getContainerToolbarHtml(container);
        container.parentNode.insertBefore(containerToolbar, container);
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
                    const validationError: ValidationError = this.getContentletValidationError(
                        item,
                        container
                    );

                    return {
                        label: this.dotMessageService.get(
                            `editpage.content.container.menu.${item}`
                        ),
                        dataset: {
                            action: 'add',
                            add: item,
                            identifier: container.dataset.dotIdentifier,
                            uuid: container.dataset.dotUuid
                        },
                        disabled:
                            validationError === ValidationError.EnterpriseLicenseError ||
                            validationError === ValidationError.MaxContentletsLimitReachedError,
                        tooltip: this.getTooltipErrorMessage(validationError)
                    };
                })
        });
    }

    private getContentletValidationError(item: string, container: HTMLElement): ValidationError {
        if (item === 'form' && !this.isEnterpriseLicense) {
            return ValidationError.EnterpriseLicenseError;
        } else if (this.isMaxContentletsLimitReached(container)) {
            return ValidationError.MaxContentletsLimitReachedError;
        }
    }

    private isMaxContentletsLimitReached(container: HTMLElement): boolean {
        const contentletsSize = Array.from(
            container.querySelectorAll('[data-dot-object="contentlet"]')
        ).length;

        return parseInt(container.dataset.maxContentlets, 10) <= contentletsSize;
    }

    private getTooltipErrorMessage(validationError: ValidationError): string {
        let errorMsg = '';
        if (validationError === ValidationError.EnterpriseLicenseError) {
            errorMsg = this.dotMessageService.get('dot.common.license.enterprise.only.error');
        } else if (validationError === ValidationError.MaxContentletsLimitReachedError) {
            errorMsg = this.dotMessageService.get('dot.common.contentlet.max.limit.error');
        }

        return errorMsg;
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
                data-dot-object="popup-button"
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
                            <li class="dotedit-menu__item ${
                                item.disabled ? 'dotedit-menu__item--disabled' : ''
                            }"
                                ${item.tooltip ? 'dot-title="' + item.tooltip + '"' : ''}">
                                    <a
                                        data-dot-object="popup-menu-item"
                                        ${this.getDotEditPopupMenuItemDataSet(
                                            item.dataset
                                        )} role="button">
                                        ${item.label}
                                    </a>
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
