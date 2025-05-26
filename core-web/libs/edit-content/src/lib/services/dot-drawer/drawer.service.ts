import { DOCUMENT } from '@angular/common';
import { ApplicationRef, computed, createComponent, EnvironmentInjector, inject, Injectable, signal } from '@angular/core';

import { DotDrawerContainerComponent } from './drawer-container.component';
import { DotDrawerConfig, DotDrawerOptions, DotDrawerOptionsOfComponent, DotDrawerResult } from './drawer-options.interface';
import { DotDrawerRef } from './drawer-ref';

/**
 * Service for managing drawer instances
 * Inspired by NgZorro's drawer service but adapted for PrimeNG
 */
@Injectable({
    providedIn: 'root'
})
export class DotDrawerService {
    private readonly applicationRef = inject(ApplicationRef);
    private readonly environmentInjector = inject(EnvironmentInjector);
    private readonly document = inject(DOCUMENT);

    // Store all drawer instances
    private readonly drawers = signal<Map<string, DotDrawerConfig>>(new Map());
    private readonly drawerRefs = new Map<string, DotDrawerRef>();
    private drawerContainer?: DotDrawerContainerComponent;
    private drawerId = 0;

    // Current active drawer
    readonly activeDrawer = computed(() => {
        const drawersMap = this.drawers();
        for (const [id, config] of drawersMap.entries()) {
            if (config.visible) {
                return { id, config };
            }
        }
        return null;
    });

    constructor() {
        this.initializeContainer();
    }

    /**
     * Create a new drawer
     */
    create<T = any, D = any, R = any>(
        options: DotDrawerOptionsOfComponent<T, D>
    ): DotDrawerRef<T, R> {
        const id = this.generateDrawerId();

        // Create drawer configuration
        const config: DotDrawerConfig<T, D> = {
            visible: false,
            options
        };

        // Create drawer reference
        const drawerRef = new DotDrawerRef<T, R>(this, id, options);

        // Store configuration and reference
        this.drawers.update(drawers => new Map(drawers.set(id, config)));
        this.drawerRefs.set(id, drawerRef);

        // Show the drawer
        this.showDrawer(id);

        return drawerRef;
    }

    /**
     * Close a specific drawer
     */
    closeDrawer(drawerId: string, result?: DotDrawerResult): void {
        const drawerRef = this.drawerRefs.get(drawerId);
        if (!drawerRef) return;

        // Handle onCancel callback if present
        const config = this.drawers().get(drawerId);
        if (config?.options.nzOnCancel && result?.type === 'cancel') {
            const cancelResult = config.options.nzOnCancel();

            if (cancelResult instanceof Promise) {
                cancelResult.then(canClose => {
                    if (canClose !== false) {
                        this.finalizeDrawerClose(drawerId, result);
                    }
                });
                return;
            }

            if (typeof cancelResult === 'boolean' && !cancelResult) {
                return; // Prevent closing
            }
        }

        this.finalizeDrawerClose(drawerId, result || { type: 'close' });
    }

    /**
     * Update drawer options
     */
    updateDrawerOptions(drawerId: string, options: Partial<DotDrawerOptions>): void {
        this.drawers.update(drawers => {
            const newDrawers = new Map(drawers);
            const config = newDrawers.get(drawerId);
            if (config) {
                newDrawers.set(drawerId, {
                    ...config,
                    options: { ...config.options, ...options }
                });
            }
            return newDrawers;
        });

        this.updateContainerConfig();
    }

    /**
     * Check if a drawer is open
     */
    isDrawerOpen(drawerId: string): boolean {
        return this.drawers().get(drawerId)?.visible ?? false;
    }

    /**
     * Trigger after open event
     */
    triggerAfterOpen(drawerId: string): void {
        const drawerRef = this.drawerRefs.get(drawerId);
        drawerRef?.triggerAfterOpen();
    }

    /**
     * Handle drawer close from container
     */
    handleDrawerClose(drawerId: string, result: DotDrawerResult): void {
        this.closeDrawer(drawerId, result);
    }

    /**
     * Show a drawer
     */
    private showDrawer(drawerId: string): void {
        // Close any other open drawer first (single drawer at a time)
        this.closeAllDrawers();

        // Show the requested drawer
        this.drawers.update(drawers => {
            const newDrawers = new Map(drawers);
            const config = newDrawers.get(drawerId);
            if (config) {
                newDrawers.set(drawerId, { ...config, visible: true });
            }
            return newDrawers;
        });

        this.updateContainerConfig();
    }

    /**
     * Close all open drawers
     */
    private closeAllDrawers(): void {
        this.drawers.update(drawers => {
            const newDrawers = new Map();
            for (const [id, config] of drawers.entries()) {
                newDrawers.set(id, { ...config, visible: false });
            }
            return newDrawers;
        });
    }

    /**
     * Finalize drawer close
     */
    private finalizeDrawerClose(drawerId: string, result: DotDrawerResult): void {
        const drawerRef = this.drawerRefs.get(drawerId);

        // Update visibility
        this.drawers.update(drawers => {
            const newDrawers = new Map(drawers);
            const config = newDrawers.get(drawerId);
            if (config) {
                newDrawers.set(drawerId, { ...config, visible: false });
            }
            return newDrawers;
        });

        // Trigger after close event
        drawerRef?.triggerAfterClose(result);

        // Clean up if destroy on close
        const config = this.drawers().get(drawerId);
        if (config?.options.nzDestroyOnClose !== false) {
            setTimeout(() => {
                this.destroyDrawer(drawerId);
            }, 300); // Allow animation to complete
        }

        this.updateContainerConfig();
    }

    /**
     * Destroy a drawer completely
     */
    private destroyDrawer(drawerId: string): void {
        this.drawers.update(drawers => {
            const newDrawers = new Map(drawers);
            newDrawers.delete(drawerId);
            return newDrawers;
        });
        this.drawerRefs.delete(drawerId);
    }

    /**
     * Update container configuration
     */
    private updateContainerConfig(): void {
        const active = this.activeDrawer();
        if (this.drawerContainer) {
            if (active) {
                // Update with active drawer
                this.drawerContainer.setConfig(active.id, active.config);
            } else {
                // No active drawer - update with empty/closed state
                this.drawerContainer.updateConfig({ visible: false });
            }
        }
    }

    /**
     * Generate unique drawer ID
     */
    private generateDrawerId(): string {
        return `drawer_${++this.drawerId}_${Date.now()}`;
    }

    /**
     * Initialize the drawer container
     */
    private initializeContainer(): void {
        const componentRef = createComponent(DotDrawerContainerComponent, {
            environmentInjector: this.environmentInjector
        });

        this.drawerContainer = componentRef.instance;
        this.drawerContainer.setDrawerService(this);
        this.applicationRef.attachView(componentRef.hostView);

        // Append to body
        const domElement = (componentRef.hostView as any).rootNodes[0] as HTMLElement;
        this.document.body.appendChild(domElement);
    }
}
