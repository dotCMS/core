import { SideMenuComponent } from "@components/sideMenu.component";
import { Page } from "@playwright/test";

export class LoginPage {
  constructor(private page: Page) {}

  /**
   *  Login to dotCMS
   * @param page
   * @param username
   * @param password
   */
  async login(username: string, password: string) {
    await this.page.goto("/dotAdmin");
    await this.page.waitForLoadState();

    const userNameInputLocator = this.page.locator('input[id="inputtext"]');
    await userNameInputLocator.fill(username);

    const passwordInputLocator = this.page.locator('input[id="password"]');
    await passwordInputLocator.fill(password);

    const loginBtnLocator = this.page.getByTestId("submitButton");
    await loginBtnLocator.click();
  }

  async loginAndOpenSideMenu(username: string, password: string) {
    await this.login(username, password);

    const sideMenu = new SideMenuComponent(this.page);
    await sideMenu.openMenu();
  }
}
