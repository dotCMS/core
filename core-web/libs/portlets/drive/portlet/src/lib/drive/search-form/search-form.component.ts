import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';

interface ContentType {
  name: string;
  value: number;
}

@Component({
  selector: 'lib-search-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    InputTextModule,
    MultiSelectModule
  ],
  templateUrl: './search-form.component.html',
  styleUrl: './search-form.component.scss'
})
export class SearchFormComponent implements OnInit {
  @Output() search = new EventEmitter<{ searchQuery: string; selectedTypes: ContentType[] }>();

  contentTypes: ContentType[] = [];
  selectedTypes: ContentType[] = [];
  searchQuery = '';

  ngOnInit(): void {
    this.contentTypes = [
      { name: 'Content', value: 1 },
      { name: 'Pages', value: 2 },
      { name: 'Language Variables', value: 3 },
      { name: 'Widgets', value: 4 },
      { name: 'Files', value: 5 }
    ];
  }

  onValueChange(form: NgForm): void {
    if (form.valid) {
      const formData = {
        searchQuery: this.searchQuery,
        selectedTypes: this.selectedTypes
      };
      this.search.emit(formData);
    }
  }
}
