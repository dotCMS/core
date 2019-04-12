import { newE2EPage } from '@stencil/core/testing';

xdescribe('dot-checkbox', () => {
  it('renders', async () => {
    const page = await newE2EPage();

    await page.setContent('<dot-checkbox></dot-checkbox>');
    const element = await page.find('dot-checkbox');
    expect(element).toHaveClass('hydrated');
  });
});
