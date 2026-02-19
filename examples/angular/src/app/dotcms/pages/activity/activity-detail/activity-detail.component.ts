import { Component, computed, input, OnChanges, signal } from '@angular/core';

import { ActivityContentlet } from '../activity.component';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-activity-detail',
  imports: [NgOptimizedImage],
  templateUrl: './activity-detail.component.html',
  styleUrl: './activity-detail.component.css',
})
export class ActivityDetailComponent {
  activity = input.required<ActivityContentlet>();

  activityContent = computed(() => {
    return this.activity().body;
  });

  activityTags = computed(() => {
    return this.activity().tags?.split(',') || [];
  });
}
