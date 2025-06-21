import { NgComponentOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ComponentRef,
    computed,
    inject,
    Injector,
    OnDestroy,
    signal,
    Type,
    ViewChild,
    ViewContainerRef
} from '@angular/core';

import { SidebarModule } from 'primeng/sidebar';

import { DotDrawerConfig, DotDrawerResult } from './drawer-options.interface';

@Component({
    selector: 'dot-drawer-container',
    standalone: true,
    imports: [SidebarModule, NgComponentOutlet],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <p-sidebar
            [visible]="visible()"
            [position]="position()"
            [modal]="modal()"
            [closeOnEscape]="closeOnEscape()"
            [dismissible]="maskClosable()"
            [showCloseIcon]="closable()"
            [blockScroll]="true"
            [styleClass]="wrapClassName()"
            [style]="sidebarStyles()"
            (onHide)="onHide()"
            (onShow)="onShow()">

            <!-- Header -->
            @if (title()) {
                <ng-template pTemplate="header">
                    <div class="drawer-header" data-testid="drawer-header">
                        <h3 class="drawer-header__title" data-testid="drawer-title">{{ title() }}</h3>
                        @if (extra()) {
                            <div class="drawer-header__extra" data-testid="drawer-extra">
                                {{ extra() }}
                            </div>
                        }
                    </div>
                </ng-template>
            }

            <!-- Content -->
            <div
                class="drawer-content"
                data-testid="drawer-content"
                [style]="bodyStyle()">
                @if (currentComponent()) {
                    <ng-container
                        *ngComponentOutlet="
                            currentComponent();
                            injector: injector
                        ">
                    </ng-container>
                } @else if (stringContent()) {
                    <div [innerHTML]="stringContent()"></div>
                }

                <!-- Fallback for ViewContainerRef -->
                <ng-container #dynamicContent></ng-container>
            </div>
        </p-sidebar>
    `,
    styles: [`
        @use "variables" as *;

        :host ::ng-deep .p-sidebar {
            .p-sidebar-header {
                padding: $spacing-3;
                border-bottom: 1px solid $color-palette-gray-300;
            }

            .p-sidebar-content {
                padding: 0;
                height: 100%;
                overflow: hidden;
            }
        }

        .drawer-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            width: 100%;

            &__title {
                margin: 0;
                font-size: $font-size-lg;
                font-weight: 600;
                color: $color-palette-gray-700;
            }

            &__extra {
                margin-left: $spacing-2;
            }
        }

        .drawer-content {
            height: 100%;
            overflow: auto;
            padding: $spacing-3;
        }
    `],
})
export class DotDrawerContainerComponent implements OnDestroy {
    @ViewChild('dynamicContent', { read: ViewContainerRef, static: true })
    dynamicContent!: ViewContainerRef;

    readonly injector = inject(Injector);

    // Signals for drawer state
    readonly config = signal<DotDrawerConfig | null>(null);

    // Computed properties from config
    readonly visible = computed(() => this.config()?.visible ?? false);
    readonly options = computed(() => this.config()?.options ?? {});

    // Drawer properties
    readonly position = computed(() => this.options().nzPlacement ?? 'right');
    readonly title = computed(() => this.options().nzTitle as string);
    readonly extra = computed(() => this.options().nzExtra as string);
    readonly closable = computed(() => this.options().nzClosable ?? true);
    readonly modal = computed(() => this.options().nzMask ?? true);
    readonly maskClosable = computed(() => this.options().nzMaskClosable ?? true);
    readonly closeOnEscape = computed(() => true);
    readonly wrapClassName = computed(() => {
        const baseClass = 'dot-drawer-container';
        const customClass = this.options().nzWrapClassName;
        return customClass ? `${baseClass} ${customClass}` : baseClass;
    });

    readonly sidebarStyles = computed(() => {
        const options = this.options();
        const styles: Record<string, string> = {};

        if (options.nzWidth) {
            styles['width'] = typeof options.nzWidth === 'number'
                ? `${options.nzWidth}px`
                : options.nzWidth;
        }

        if (options.nzHeight) {
            styles['height'] = typeof options.nzHeight === 'number'
                ? `${options.nzHeight}px`
                : options.nzHeight;
        }

        if (options.nzZIndex) {
            styles['z-index'] = options.nzZIndex.toString();
        }

        return styles;
    });

    readonly bodyStyle = computed(() => this.options().nzBodyStyle ?? {});

    // Content management
    readonly currentComponent = computed(() => {
        const content = this.options().nzContent;
        return (typeof content === 'function' && content.prototype?.constructor)
            ? content as Type<any>
            : null;
    });

    readonly stringContent = computed(() => {
        const content = this.options().nzContent;
        return typeof content === 'string' ? content : null;
    });

    private componentRef?: ComponentRef<any>;
    private drawerId?: string;
    private drawerService?: any; // Will be set externally

    ngOnDestroy(): void {
        this.destroyComponent();
    }

    /**
     * Set the drawer service reference
     */
    setDrawerService(service: any): void {
        this.drawerService = service;
    }

    /**
     * Set the drawer configuration
     */
    setConfig(drawerId: string, config: DotDrawerConfig): void {
        this.drawerId = drawerId;
        this.config.set(config);
        this.createComponent();
    }

    /**
     * Update drawer configuration
     */
    updateConfig(config: Partial<DotDrawerConfig>): void {
        const currentConfig = this.config();
        if (currentConfig) {
            const newConfig = { ...currentConfig, ...config };
            this.config.set(newConfig);
        } else if (config.visible === false) {
            // Handle closing when no current config
            this.config.set({
                visible: false,
                options: {}
            });
        }
    }

    /**
     * Handle sidebar show event
     */
    onShow(): void {
        if (this.drawerId && this.drawerService) {
            this.drawerService.triggerAfterOpen(this.drawerId);
        }
    }

    /**
     * Handle sidebar hide event
     */
    onHide(): void {
        console.log('🚪 Sidebar hide event triggered');

        // First update local state
        const currentConfig = this.config();
        if (currentConfig) {
            this.config.set({ ...currentConfig, visible: false });
        }

        // Then handle close through service
        this.handleClose({ type: 'cancel' });
    }

    /**
     * Handle drawer close with result
     */
    handleClose(result: DotDrawerResult): void {
        if (this.drawerId && this.drawerService) {
            this.drawerService.handleDrawerClose(this.drawerId, result);
        }
    }

    /**
     * Create dynamic component
     */
    private createComponent(): void {
        const options = this.options();
        const componentType = this.currentComponent();

        if (componentType && this.dynamicContent) {
            this.destroyComponent();

            this.componentRef = this.dynamicContent.createComponent(componentType);

            // Pass content params as component inputs
            if (options.nzContentParams && this.componentRef.instance) {
                const params = options.nzContentParams;
                console.log('🔧 Setting component inputs:', params);

                // Set component inputs using setInput() for input signals
                if (this.componentRef.setInput) {
                    Object.keys(params).forEach(key => {
                        console.log(`Setting input: ${key} = ${params[key]}`);
                        this.componentRef.setInput(key, params[key]);
                    });
                } else {
                    // Fallback: direct assignment to instance properties
                    console.log('Using fallback: direct assignment');
                    Object.assign(this.componentRef.instance, params);
                }

                // Trigger change detection
                this.componentRef.changeDetectorRef.markForCheck();
                this.componentRef.changeDetectorRef.detectChanges();
            }

            // Store component ref in config for external access
            const currentConfig = this.config();
            if (currentConfig) {
                this.config.set({
                    ...currentConfig,
                    componentRef: this.componentRef
                });
            }
        }
    }

    /**
     * Destroy current component
     */
    private destroyComponent(): void {
        if (this.componentRef) {
            this.componentRef.destroy();
            this.componentRef = undefined;
        }
    }
}
