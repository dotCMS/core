import { Component, computed, input } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { editContentlet } from '@dotcms/uve';
import { Blog } from '../../../../shared/contentlet.model';
import { UVE_MODE } from '@dotcms/types';
import { DotCMSShowWhenDirective } from '@dotcms/angular';
@Component({
  selector: 'app-blog-card',
  templateUrl: './blog-card.component.html',
  imports: [RouterLink, NgOptimizedImage, DotCMSShowWhenDirective],
})
export class BlogCardComponent {
  blog = input.required<Blog>();

  UVE_MODE = UVE_MODE;

  editContentlet(blog: Blog): void {
    editContentlet(blog);
  }

  formattedDate = computed(() => this.getFormattedDate(this.blog().modDate));

  getFormattedDate(dateString: string): string {
    const options: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    };

    return new Date(dateString).toLocaleDateString('en-US', options);
  }
}
