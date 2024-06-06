import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DotcmsLayoutComponent } from '@dotcms/angular';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, DotcmsLayoutComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {}
