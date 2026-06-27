import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-search-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search-input.component.html',
  styleUrls: ['./search-input.component.scss'],
})
export class SearchInputComponent {
  @Input() searchTerm: string = '';
  @Input() placeholder: string = 'Search...';
  @Input() showDrawButton: boolean = false;

  @Output() searchTermChange = new EventEmitter<string>();
  @Output() openDraw = new EventEmitter<void>();
  @Output() search = new EventEmitter<string>();

  onInput() {
    this.searchTermChange.emit(this.searchTerm);
    this.search.emit(this.searchTerm);
  }

  onClear() {
    this.searchTerm = '';
    this.onInput();
  }

  onOpenDraw() {
    this.openDraw.emit();
  }
}
