import { DotSiteBrowserModule } from './dot-site-browser.module';

describe('DotSiteBrowserModule', () => {
  let dotSiteBrowserModule: DotSiteBrowserModule;

  beforeEach(() => {
    dotSiteBrowserModule = new DotSiteBrowserModule();
  });

  it('should create an instance', () => {
    expect(dotSiteBrowserModule).toBeTruthy();
  });
});
