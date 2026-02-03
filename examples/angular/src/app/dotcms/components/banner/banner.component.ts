import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DotCMSEditableTextComponent } from '@dotcms/angular';

import { Banner } from '../../types/contentlet.model';

interface BannerDotStyleProperties {
  'title-size'?: string;
  'caption-size'?: string;
  'title-style'?: {
    bold?: boolean;
    italic?: boolean;
    underline?: boolean;
  };
  'text-alignment'?: 'left' | 'center' | 'right';
  'overlay-style'?: 'none' | 'dark' | 'light' | 'gradient';
  'button-color'?: 'blue' | 'green' | 'red' | 'purple' | 'orange' | 'teal';
  'button-size'?: 'small' | 'medium' | 'large';
  'button-style'?: {
    rounded?: boolean;
    'full-rounded'?: boolean;
    shadow?: boolean;
  };
}

@Component({
  selector: 'app-banner',
  imports: [RouterLink, NgOptimizedImage, DotCMSEditableTextComponent],
  template: `<div class="relative w-full p-4 bg-gray-200 h-96">
    @if (contentlet().image.identifier; as imageIdentifier) {
      <img
        class="object-cover w-full"
        [ngSrc]="imageIdentifier"
        [alt]="contentlet().title"
        fill
        priority
      />
    }
    @if (overlayStyle() !== 'none') {
      <div [class]="overlayClasses()"></div>
    }
    <div [class]="contentContainerClasses()">
      <h2 [class]="titleClasses()">
        <dotcms-editable-text fieldName="title" [contentlet]="contentlet()" />
      </h2>
      @if (contentlet().caption) {
        <p [class]="captionClasses()">{{ contentlet().caption }}</p>
      }
      @if (contentlet().link) {
        <a
          [class]="buttonClasses()"
          [routerLink]="contentlet().link"
        >
          {{ contentlet().buttonText || 'See more' }}
        </a>
      }
    </div>
  </div>`,
  styleUrl: './banner.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BannerComponent {
  contentlet = input.required<Banner>();

  dotStyleProperties = computed(() => this.contentlet().dotStyleProperties as BannerDotStyleProperties);

  // Computed properties for style values with defaults
  titleSize = computed(() => this.dotStyleProperties()?.['title-size'] || 'text-6xl');
  captionSize = computed(() => this.dotStyleProperties()?.['caption-size'] || 'text-xl');
  titleStyle = computed(() => this.dotStyleProperties()?.['title-style'] || {});
  textAlignment = computed(() => this.dotStyleProperties()?.['text-alignment'] || 'center');
  overlayStyle = computed(() => this.dotStyleProperties()?.['overlay-style'] || 'none');
  buttonColor = computed(() => this.dotStyleProperties()?.['button-color'] || 'blue');
  buttonSize = computed(() => this.dotStyleProperties()?.['button-size'] || 'medium');
  buttonStyle = computed(() => this.dotStyleProperties()?.['button-style'] || {});

  // Computed classes
  titleClasses = computed(() => {
    const style = this.titleStyle();
    const classes = [
      'mb-2',
      this.titleSize(),
      style.bold ? 'font-bold' : 'font-normal',
      style.italic ? 'italic' : '',
      style.underline ? 'underline' : '',
      'text-white',
      'text-shadow'
    ].filter(Boolean);
    return classes.join(' ');
  });

  captionClasses = computed(() => {
    return ['mb-4', this.captionSize(), 'text-white', 'text-shadow'].join(' ');
  });

  alignmentClasses = computed(() => {
    switch (this.textAlignment()) {
      case 'left':
        return 'items-start text-left';
      case 'right':
        return 'items-end text-right';
      case 'center':
      default:
        return 'items-center text-center';
    }
  });

  contentContainerClasses = computed(() => {
    return [
      'absolute',
      'inset-0',
      'flex',
      'flex-col',
      'justify-center',
      'p-4',
      this.alignmentClasses(),
      'text-white'
    ].join(' ');
  });

  overlayClasses = computed(() => {
    switch (this.overlayStyle()) {
      case 'dark':
        return 'absolute inset-0 bg-black/40';
      case 'light':
        return 'absolute inset-0 bg-white/20';
      case 'gradient':
        return 'absolute inset-0 bg-gradient-to-b from-black/50 via-transparent to-black/50';
      case 'none':
      default:
        return '';
    }
  });

  buttonColorClasses = computed(() => {
    const colorMap: Record<string, string> = {
      blue: 'bg-blue-500 hover:bg-blue-700',
      green: 'bg-green-500 hover:bg-green-700',
      red: 'bg-red-500 hover:bg-red-700',
      purple: 'bg-purple-500 hover:bg-purple-700',
      orange: 'bg-orange-500 hover:bg-orange-700',
      teal: 'bg-teal-500 hover:bg-teal-700'
    };
    return colorMap[this.buttonColor()] || colorMap['blue'];
  });

  buttonSizeClasses = computed(() => {
    switch (this.buttonSize()) {
      case 'small':
        return 'px-3 py-2 text-base';
      case 'large':
        return 'px-6 py-4 text-2xl';
      case 'medium':
      default:
        return 'px-4 py-2 text-xl';
    }
  });

  buttonStyleClasses = computed(() => {
    const style = this.buttonStyle();
    const classes: string[] = [];
    
    if (style.rounded) {
      classes.push('rounded-lg');
    } else if (style['full-rounded']) {
      classes.push('rounded-full');
    } else {
      classes.push('rounded-sm');
    }
    
    if (style.shadow) {
      classes.push('shadow-lg');
    }
    
    return classes.join(' ');
  });

  buttonClasses = computed(() => {
    return [
      'transition',
      'duration-300',
      'text-white',
      'font-bold',
      this.buttonColorClasses(),
      this.buttonSizeClasses(),
      this.buttonStyleClasses()
    ].join(' ');
  });
}
