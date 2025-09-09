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
    await this.page.waitForLoadState();

    // Debug: Check if Angular is loaded
    const angularLoaded = await this.page.evaluate(() => {
      return typeof window !== 'undefined' && 
             (window as any).ng !== undefined &&
             document.querySelector('dot-login-component') !== null;
    });
    console.log('Angular loaded:', angularLoaded);

    // Debug: Wait for Angular to be ready
    if (angularLoaded) {
      await this.page.waitForFunction(() => {
        const loginComponent = document.querySelector('dot-login-component');
        return loginComponent && loginComponent.querySelector('form');
      });
    }

    const userNameInputLocator = this.page.locator('input[id="inputtext"]');
    await userNameInputLocator.fill(username);

    const passwordInputLocator = this.page.locator('input[id="password"]');
    await passwordInputLocator.fill(password);

    // Debug: Check form state before clicking
    const formDebugInfo = await this.page.evaluate(() => {
      const form = document.querySelector('form');
      const button = document.querySelector('[data-testid="submitButton"]') as HTMLButtonElement;
      const usernameInput = document.querySelector('input[id="inputtext"]') as HTMLInputElement;
      const passwordInput = document.querySelector('input[id="password"]') as HTMLInputElement;
      
      return {
        formExists: !!form,
        buttonExists: !!button,
        buttonDisabled: button?.disabled,
        buttonClass: button?.className,
        usernameValue: usernameInput?.value,
        passwordValue: passwordInput ? '***hidden***' : 'not found',
        formClass: form?.className,
        angularFormValid: (window as any).ng ? 'Angular available' : 'Angular not available'
      };
    });
    
    console.log('Form debug info:', JSON.stringify(formDebugInfo, null, 2));

    const loginBtnLocator = this.page.getByTestId("submitButton");
    await loginBtnLocator.click();
  }

  async loginAndOpenSideMenu(username: string, password: string) {
    await this.login(username, password);

    const sideMenu = new SideMenuComponent(this.page);
    await sideMenu.openMenu();
  }
}
