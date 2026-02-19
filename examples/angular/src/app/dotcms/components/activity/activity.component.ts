import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Activity } from '../../types/contentlet.model';

interface DotStyleProperties {
  'title-size'?: string;
  'description-size'?: string;
  'title-style'?: {
    bold?: boolean;
    italic?: boolean;
    underline?: boolean;
  };
  layout?: 'left' | 'right' | 'center' | 'overlap';
  'image-height'?: string;
  'card-background'?: 'white' | 'gray' | 'light-blue' | 'light-green';
  'border-radius'?: 'none' | 'small' | 'medium' | 'large';
  'card-effects'?: {
    shadow?: boolean;
    border?: boolean;
  };
  'button-color'?: 'blue' | 'green' | 'red' | 'purple' | 'orange' | 'teal';
  'button-size'?: 'small' | 'medium' | 'large';
  'button-style'?: {
    rounded?: boolean;
    'full-rounded'?: boolean;
    shadow?: boolean;
  };
}

@Component({
  selector: 'app-activity',
  imports: [RouterLink, NgOptimizedImage],
  template: `
    <article [class]="articleClasses()">
      @if (contentlet().inode; as inode) {
        <div [class]="imageContainerClasses()">
          <div [class]="imageWrapperClasses()">
            <img
              class="object-cover w-full h-full"
              [ngSrc]="inode"
              width="400"
              height="300"
              alt="Activity Image"
            />
          </div>
        </div>
      }
      <div [class]="contentContainerClasses()">
        <p [class]="titleClasses()">{{ contentlet().title }}</p>
        <p [class]="descriptionClasses()">{{ contentlet().description }}</p>
        <div [class.flex]="layout() === 'center'" [class.justify-center]="layout() === 'center'">
          <a
            [routerLink]="'/activities/' + (contentlet().urlTitle || '#')"
            [class]="buttonClasses()"
          >
            Link to detail →
          </a>
        </div>
      </div>
    </article>
  `,
  styleUrl: './activity.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityComponent {
  contentlet = input.required<Activity>();

  dotStyleProperties = computed(() => this.contentlet().dotStyleProperties as DotStyleProperties);

  // Computed properties for style values
  titleSize = computed(() => this.dotStyleProperties()?.['title-size'] || 'text-xl');
  descriptionSize = computed(() => this.dotStyleProperties()?.['description-size'] || 'text-base');
  titleStyle = computed(() => this.dotStyleProperties()?.['title-style'] || {});
  layout = computed(() => this.dotStyleProperties()?.layout || 'left');
  imageHeight = computed(() => this.dotStyleProperties()?.['image-height'] || 'h-56');
  cardBackground = computed(() => this.dotStyleProperties()?.['card-background'] || 'white');
  borderRadius = computed(() => this.dotStyleProperties()?.['border-radius'] || 'small');
  cardEffects = computed(() => this.dotStyleProperties()?.['card-effects'] || {});
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
    ].filter(Boolean);
    return classes.join(' ');
  });

  descriptionClasses = computed(() => {
    return `${this.descriptionSize()} line-clamp-3 mb-4`;
  });

  layoutClasses = computed(() => {
    const layoutValue = this.layout();
    switch (layoutValue) {
      case 'right':
        return 'flex flex-row-reverse';
      case 'center':
        return 'flex flex-col items-center text-center';
      case 'overlap':
        return 'relative min-h-96';
      case 'left':
      default:
        return 'flex flex-row';
    }
  });

  imageContainerClasses = computed(() => {
    const layoutValue = this.layout();
    switch (layoutValue) {
      case 'overlap':
        return 'absolute inset-0 z-0';
      case 'center':
        return 'relative w-full';
      default:
        return 'relative flex-shrink-0 w-1/2';
    }
  });

  imageWrapperClasses = computed(() => {
    return `relative w-full overflow-hidden ${this.layout() === 'overlap' ? 'h-full' : this.imageHeight()}`;
  });

  contentContainerClasses = computed(() => {
    const layoutValue = this.layout();
    switch (layoutValue) {
      case 'overlap':
        return 'relative z-10 p-8 bg-white/90 min-h-96 flex flex-col justify-center';
      case 'center':
        return 'w-full px-6 py-4';
      default:
        return 'flex-1 p-6 flex flex-col justify-center';
    }
  });

  cardBackgroundClasses = computed(() => {
    const bgMap: Record<string, string> = {
      white: 'bg-white',
      gray: 'bg-gray-100',
      'light-blue': 'bg-blue-50',
      'light-green': 'bg-green-50',
    };
    return bgMap[this.cardBackground()] || bgMap['white'];
  });

  borderRadiusClasses = computed(() => {
    const radiusMap: Record<string, string> = {
      none: 'rounded-none',
      small: 'rounded-sm',
      medium: 'rounded-md',
      large: 'rounded-lg',
    };
    return radiusMap[this.borderRadius()] || radiusMap['small'];
  });

  cardEffectClasses = computed(() => {
    const effects = this.cardEffects();
    const shadow = effects.shadow ? 'shadow-lg' : 'shadow-md';
    const border = effects.border ? 'border border-gray-200' : '';
    return `${shadow} ${border}`.trim();
  });

  buttonColorClasses = computed(() => {
    const colorMap: Record<string, string> = {
      blue: 'bg-blue-500 hover:bg-blue-700',
      green: 'bg-green-500 hover:bg-green-700',
      red: 'bg-red-500 hover:bg-red-700',
      purple: 'bg-purple-500 hover:bg-purple-700',
      orange: 'bg-orange-500 hover:bg-orange-700',
      teal: 'bg-teal-500 hover:bg-teal-700',
    };
    return colorMap[this.buttonColor()] || colorMap['blue'];
  });

  buttonSizeClasses = computed(() => {
    const size = this.buttonSize();
    switch (size) {
      case 'small':
        return 'px-3 py-1.5 text-sm';
      case 'large':
        return 'px-6 py-3 text-lg';
      case 'medium':
      default:
        return 'px-4 py-2 text-base';
    }
  });

  buttonStyleClasses = computed(() => {
    const style = this.buttonStyle();
    const rounded = style.rounded ? 'rounded-lg' : 'rounded-full';
    const shadow = style.shadow ? 'shadow-lg' : '';
    return `${rounded} ${shadow}`.trim();
  });

  buttonClasses = computed(() => {
    return `inline-block font-bold text-white transition duration-300 ${this.buttonColorClasses()} ${this.buttonSizeClasses()} ${this.buttonStyleClasses()}`;
  });

  cardClasses = computed(() => {
    return `overflow-hidden mb-4 ${this.cardBackgroundClasses()} ${this.borderRadiusClasses()} ${this.cardEffectClasses()}`;
  });

  articleClasses = computed(() => {
    const layoutValue = this.layout();
    const paddingClass = layoutValue === 'overlap' ? 'p-0' : 'p-4';
    return `${paddingClass} ${this.cardClasses()} ${this.layoutClasses()}`;
  });
}
