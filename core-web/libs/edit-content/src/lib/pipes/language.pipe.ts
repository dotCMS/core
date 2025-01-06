import { Pipe, PipeTransform } from '@angular/core';

import { DotLanguage } from '@dotcms/dotcms-models';

@Pipe({
  name: 'language',
  standalone: true
})
export class LanguagePipe implements PipeTransform {
  transform(language: DotLanguage): string {
    if (!language) {
      return '';
    }

    const code = language.languageCode || language.isoCode || '';

    return `${language.language} (${code})`;
  }
}
