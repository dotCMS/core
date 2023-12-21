import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { ActionPayload } from '../../../shared/models';
import { ContentletArea } from '../ema-page-dropzone/ema-page-dropzone.component';

@Component({
    selector: 'dot-ema-contentlet-tools',
    standalone: true,
    imports: [CommonModule, ButtonModule, MenuModule],
    templateUrl: './ema-contentlet-tools.component.html',
    styleUrls: ['./ema-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaContentletToolsComponent {
    private buttonPosition: 'after' | 'before' = 'after';

    @Input() contentlet: ContentletArea;
    @Output() add = new EventEmitter<ActionPayload>();
    @Output() edit = new EventEmitter<ActionPayload>();
    @Output() delete = new EventEmitter<ActionPayload>();

    items: MenuItem[] = [
        {
            label: 'Content',
            icon: 'pi pi-refresh',
            command: () => {
                this.add.emit({
                    ...this.contentlet.payload,
                    position: this.buttonPosition
                });
            }
        },
        {
            label: 'Form',
            icon: 'pi pi-book',
            command: () => {
                // eslint-disable-next-line no-console
                console.log('Form');
            }
        },
        {
            label: 'Widget',
            icon: 'pi pi-cog',
            command: () => {
                // eslint-disable-next-line no-console
                console.log('Widget');
            }
        }
    ];

    editHandler() {
        this.edit.emit(this.contentlet.payload);
    }
    deleteHandler() {
        this.delete.emit(this.contentlet.payload);
    }

    setPositionFlag(position: 'before' | 'after'): void {
        this.buttonPosition = position;
    }

    getPosition(): Record<string, string> {
        return {
            left: `${this.contentlet.x}px`,
            top: `${this.contentlet.y}px`,
            width: `${this.contentlet.width}px`,
            height: `${this.contentlet.height}px`
        };
    }

    getTopButtonPosition(): Record<string, string> {
        const buttonWidth = 40;
        const buttonHeight = 40;
        const contentletCenterX = this.contentlet.x + this.contentlet.width / 2;
        const buttonLeft = contentletCenterX - buttonWidth / 2;
        const buttonTop = this.contentlet.y - buttonHeight / 2;

        return {
            position: 'absolute',
            left: `${buttonLeft}px`,
            top: `${buttonTop}px`,
            zIndex: '1'
        };
    }

    getBottomButtonPosition(): Record<string, string> {
        const buttonWidth = 40;
        const buttonHeight = 40;
        const contentletCenterX = this.contentlet.x + this.contentlet.width / 2;
        const buttonLeft = contentletCenterX - buttonWidth / 2;
        const buttonTop = this.contentlet.y + this.contentlet.height - buttonHeight / 2;

        return {
            position: 'absolute',
            left: `${buttonLeft}px`,
            top: `${buttonTop}px`,
            zIndex: '1'
        };
    }

    getActionPosition(): Record<string, string> {
        const width = 84;
        const height = 40;
        const contentletCenterX = this.contentlet.x + this.contentlet.width;
        const ledt = contentletCenterX - width - 20;
        const top = this.contentlet.y - height / 2;

        return {
            position: 'absolute',
            left: `${ledt}px`,
            top: `${top}px`,
            zIndex: '1'
        };
    }
}
