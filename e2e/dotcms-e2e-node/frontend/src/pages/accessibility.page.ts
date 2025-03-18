import * as fs from "node:fs";
import { Page } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { createHtmlReport } from "axe-html-reporter";

export class AccessibilityPage {
  constructor(private page: Page) {}

  async generateReport(description: string) {
    const accessibilityScanResults = await new AxeBuilder({
      page: this.page,
    }).analyze();
    const reportHTML = createHtmlReport({
      results: accessibilityScanResults,
      options: {
        projectKey: description,
      },
    });

    if (!fs.existsSync("build/reports")) {
      fs.mkdirSync("build/reports", {
        recursive: true,
      });
    }
    fs.writeFileSync("build/reports/accessibility-report.html", reportHTML);
    return accessibilityScanResults;
  }
}
