import { BasePage } from "./base.page";
import { Page } from "@playwright/test";

export class StarterPage extends BasePage {
  constructor(protected page: Page) {
    super(page);
  }
}
