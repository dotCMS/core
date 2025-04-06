import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';

interface BaseType {
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
  @Output() search = new EventEmitter<{ searchQuery: string; selectedTypes: BaseType[] }>();

  baseTypes: BaseType[] = [
    { name: 'Content', value: 1 },
    { name: 'Pages', value: 5 },
    { name: 'Language Variables', value: 8 },
    { name: 'Widgets', value: 2 },
    { name: 'Files', value: 4 },
    { name: 'Personas', value: 6 },
    { name: 'Vanity URLs', value: 7 },
    { name: 'Dot Assets', value: 9 }
  ];
  selectedTypes: BaseType[] = this.baseTypes.filter(item => item.value !== 8);
  searchQuery = '';

  ngOnInit(): void {
    const formData = {
        searchQuery: this.searchQuery,
        selectedTypes: this.selectedTypes
      };

      this.search.emit(formData);
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
