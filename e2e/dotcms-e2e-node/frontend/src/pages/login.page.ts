import { SideMenuComponent } from "@components/sideMenu.component";
import { Page } from "@playwright/test";

export class LoginPage {
  constructor(private page: Page) {
    // Capture JavaScript errors
    this.page.on('pageerror', (error) => {
      console.error('JavaScript error on login page:', error.message);
      console.error('Stack:', error.stack);
    });

    // Capture console errors
    this.page.on('console', (msg) => {
      if (msg.type() === 'error') {
        console.error('Console error:', msg.text());
      }
    });

    // Capture failed network requests
    this.page.on('requestfailed', (request) => {
      console.error('Failed request:', request.url(), request.failure()?.errorText);
    });
  }

  /**
   *  Login to dotCMS
   * @param page
   * @param username
   * @param password
   */
  async login(username: string, password: string) {
    await this.page.goto("/dotAdmin");
    
    // Wait for network requests to settle (avoid resource loading race conditions)
    await this.page.waitForLoadState('networkidle');
    
    // Wait for login form to be present and ready
    await this.page.waitForSelector('dot-login-component', { timeout: 15000 });
    await this.page.waitForSelector('form', { timeout: 10000 });
    
    // Fill form inputs
    const userNameInputLocator = this.page.locator('input[id="inputtext"]');
    await userNameInputLocator.fill(username);

    const passwordInputLocator = this.page.locator('input[id="password"]');
    await passwordInputLocator.fill(password);

    // Critical: Wait for Angular reactive form validation to complete
    // This addresses the core issue where form validation doesn't complete due to resource contention
    await this.page.waitForFunction(() => {
      const button = document.querySelector('[data-testid="submitButton"]') as HTMLButtonElement;
      const usernameInput = document.querySelector('input[id="inputtext"]') as HTMLInputElement;
      const passwordInput = document.querySelector('input[id="password"]') as HTMLInputElement;
      
      // Ensure form is functionally ready, not just visually present
      return button && !button.disabled &&
             usernameInput && usernameInput.value.trim() !== '' &&
             passwordInput && passwordInput.value.trim() !== '' &&
             // Additional check: ensure button classes indicate enabled state
             !button.className.includes('p-disabled');
    }, { 
      timeout: 20000, // Generous timeout for resource-constrained environments
      polling: 500    // Check every 500ms to avoid overwhelming the system
    });

    const loginBtnLocator = this.page.getByTestId("submitButton");
    await loginBtnLocator.click();
    
    // Wait for navigation to complete after login
    await this.page.waitForURL('**/dotAdmin/**', { timeout: 15000 });
  }

  async loginAndOpenSideMenu(username: string, password: string) {
    await this.login(username, password);

    const sideMenu = new SideMenuComponent(this.page);
    await sideMenu.openMenu();
  }
}
