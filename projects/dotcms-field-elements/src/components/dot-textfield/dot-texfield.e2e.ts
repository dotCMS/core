import { newE2EPage } from '@stencil/core/testing';

describe('dot-textfield', () => {
  it('renders', async () => {
    const page = await newE2EPage();

    await page.setContent('<dot-textfield></dot-textfield>');
    const element = await page.find('dot-textfield');
    expect(element).toHaveClass('hydrated');
  });
});
