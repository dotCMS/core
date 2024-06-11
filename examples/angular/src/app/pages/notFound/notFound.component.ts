import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [],
  template: `<p>notFound works!</p>`,
  styleUrl: './notFound.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundComponent { }
