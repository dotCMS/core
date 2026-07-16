import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-loading',
  imports: [],
  template: `
    <div class="loading-container">
      <!-- Layout 1 -->
      @if (selectedLayout === 1) {
        <div class="loading-hero">
          <div class="loading-hero-headline"></div>
          <div class="loading-hero-caption"></div>
          <div class="loading-hero-cta"></div>
        </div>
        <div class="loading-articles">
          @for (i of [1, 2, 3, 4]; track i) {
            <div class="loading-article"></div>
          }
        </div>
      }

      <!-- Layout 2 -->
      @if (selectedLayout === 2) {
        <div class="loading-centered-title">
          <div class="loading-title"></div>
          <div class="loading-subtitle"></div>
        </div>
        <div class="loading-image-text">
          <div class="loading-image"></div>
          <div class="loading-text"></div>
        </div>
        <div class="loading-image-text reverse">
          <div class="loading-image"></div>
          <div class="loading-text"></div>
        </div>
      }

      <!-- Layout 3 -->
      @if (selectedLayout === 3) {
        <div class="loading-card-grid">
          @for (i of [1, 2, 3, 4, 5, 6, 7, 8]; track i) {
            <div class="loading-card"></div>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .loading-container {
        width: 100%;
        max-width: 1200px;
        margin: 0 auto;
        padding: 1rem;
      }
      .loading-hero {
        height: 400px;
        background-color: #eee;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        gap: 1rem;
        margin-bottom: 2rem;
      }
      .loading-hero-headline {
        width: 60%;
        height: 40px;
        background-color: #ddd;
        border-radius: 4px;
      }
      .loading-hero-caption {
        width: 40%;
        height: 20px;
        background-color: #ddd;
        border-radius: 4px;
      }
      .loading-hero-cta {
        width: 150px;
        height: 40px;
        background-color: #ddd;
        border-radius: 20px;
      }
      .loading-articles {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 1rem;
      }
      .loading-article {
        height: 250px;
        background-color: #eee;
        border-radius: 4px;
      }
      .loading-centered-title {
        text-align: center;
        margin-bottom: 2rem;
      }
      .loading-title {
        width: 60%;
        height: 40px;
        background-color: #eee;
        border-radius: 4px;
        margin: 0 auto 1rem;
      }
      .loading-subtitle {
        width: 40%;
        height: 20px;
        background-color: #eee;
        border-radius: 4px;
        margin: 0 auto;
      }
      .loading-image-text {
        display: flex;
        gap: 2rem;
        margin-bottom: 2rem;
      }
      .loading-image-text.reverse {
        flex-direction: row-reverse;
      }
      .loading-image {
        width: 50%;
        height: 300px;
        background-color: #eee;
        border-radius: 4px;
      }
      .loading-text {
        width: 50%;
        height: 300px;
        background-color: #eee;
        border-radius: 4px;
      }
      .loading-card-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 1rem;
      }
      .loading-card {
        height: 250px;
        background-color: #eee;
        border-radius: 4px;
      }
      .loading-logo,
      .loading-nav-item,
      .loading-hero,
      .loading-article,
      .loading-title,
      .loading-subtitle,
      .loading-image,
      .loading-text,
      .loading-card {
        position: relative;
        overflow: hidden;
      }
      .loading-logo::after,
      .loading-nav-item::after,
      .loading-hero::after,
      .loading-article::after,
      .loading-title::after,
      .loading-subtitle::after,
      .loading-image::after,
      .loading-text::after,
      .loading-card::after {
        content: '';
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        left: 0;
        background: linear-gradient(90deg, #eee, #f5f5f5, #eee);
        animation: shimmer 2s infinite;
        transform: translateX(-100%);
      }
      @keyframes shimmer {
        100% {
          transform: translateX(100%);
        }
      }
    `,
  ],
})
export class LoadingComponent implements OnInit {
  selectedLayout: number = 1;

  ngOnInit() {
    this.selectedLayout = Math.floor(Math.random() * 3) + 1;
  }
}
