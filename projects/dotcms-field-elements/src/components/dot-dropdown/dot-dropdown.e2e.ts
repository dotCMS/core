import { newE2EPage } from '@stencil/core/testing';

describe('dot-dropdown', () => {
  it('renders', async () => {
    const page = await newE2EPage();

    await page.setContent('<dot-dropdown></dot-dropdown>');
    const element = await page.find('dot-dropdown');
    expect(element).toHaveClass('hydrated');
  });
});
