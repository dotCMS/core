import {
  Component,
  computed,
  input,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';

import { BannerCarousel } from '../../types/contentlet.model';

@Component({
  selector: 'app-banner-carousel',
  templateUrl: './banner-carousel.component.html',
  imports: [],
})
export class BannerCarouselComponent implements OnInit, OnDestroy {
  banners = computed(() => this.contentlet().widgetCodeJSON?.banners || []);

  contentlet = input.required<BannerCarousel>();
  currentIndex = signal<number>(0);
  private slideInterval: any;

  ngOnInit(): void {
    this.startSlideInterval();
  }

  ngOnDestroy(): void {
    this.clearSlideInterval();
  }

  startSlideInterval(): void {
    this.slideInterval = setInterval(() => {
      this.nextSlide();
    }, 3000);
  }

  clearSlideInterval(): void {
    if (this.slideInterval) {
      clearInterval(this.slideInterval);
    }
  }

  nextSlide(): void {
    const length = this.banners()?.length || 1;
    this.currentIndex.update((index) => (index + 1) % length);
    this.clearSlideInterval();
    this.startSlideInterval();
  }

  prevSlide(): void {
    const length = this.banners()?.length || 1;
    this.currentIndex.update((index) => (index - 1 + length) % length);
    this.clearSlideInterval();
    this.startSlideInterval();
  }
}
