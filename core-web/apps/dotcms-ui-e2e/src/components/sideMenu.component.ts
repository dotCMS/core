import { Page, expect } from "@playwright/test";

export class SideMenuComponent {
    constructor(private page: Page) { }

    async openMenu() {
        // Based on codegen - click on the right content pane area to activate navigation
        const iframe = this.page.locator('iframe[name="detailFrame"]');
        if (await iframe.isVisible()) {
            const contentFrame = iframe.contentFrame();
            const rightPane = contentFrame.locator('#rightContentPane');
            if (await rightPane.isVisible()) {
                await rightPane.click();
            }
        }

        // Alternative: try to expand menu if collapsed
        await this.expandMenuIfNeeded();
    }

    async expandMenuIfNeeded() {
        // Check if we need to expand the menu by looking for collapsed state
        const menu = this.page.locator("nav[role='navigation']");
        if (await menu.isVisible()) {
            const classes = await menu.getAttribute("class");
            if (classes && classes.includes("collapsed")) {
                // Try to find and click expand button or hover over menu area
                const expandArea = this.page.locator('.nav-toggle, .menu-toggle, [aria-label*="menu"]').first();
                if (await expandArea.isVisible()) {
                    await expandArea.click();
                }
            }
        }
    }

    /**
     * Navigate to the content portlet providing the menu, group and tool locators
     * @param group
     * @param tool
     */
    async navigate(group: string, tool: string) {
        await this.openMenu();

        // Based on codegen - use getByRole for navigation links
        if (group.toLowerCase() === 'site' && tool.toLowerCase() === 'browser') {
            // Direct navigation to Browser as shown in codegen
            await this.page.getByRole('link', { name: 'Browser' }).click();
        } else {
            // Generic navigation for other menu items
            // First try to find and expand the group if needed
            const groupElement = this.page.getByText(group, { exact: true });
            if (await groupElement.isVisible()) {
                await groupElement.click();
            }

            // Then click on the tool/link
            const toolLink = this.page.getByRole("link", { name: tool });
            await expect(toolLink).toBeVisible();
            await toolLink.click();
        }
    }

    /**
     * Navigate to Site Browser specifically (based on codegen)
     */
    async navigateToSiteBrowser() {
        await this.openMenu();

        // Use the exact pattern from codegen
        await this.page.getByRole('link', { name: 'Browser' }).click();
    }

    /**
     * Handle site menu expansion (based on codegen pattern)
     */
    async expandSiteMenu() {
        // Based on codegen: page.getByText('folder_openSitearrow_drop_up').click()
        const siteMenuToggle = this.page.getByText('folder_openSitearrow_drop_up');
        if (await siteMenuToggle.isVisible()) {
            await siteMenuToggle.click();
        }
    }
}
