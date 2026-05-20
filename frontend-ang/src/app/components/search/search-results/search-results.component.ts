import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { Experiment } from '../../../dtos/experiment';

export type SearchState = 'initial' | 'no-results' | 'has-results';


@Component({
  selector: 'app-experiment-results',
  templateUrl: './search-results.component.html',
  styleUrls: ['./search-results.component.scss'],
  standalone: false,
})
export class SearchResultsComponent implements OnChanges {
  @Input() state: SearchState = 'initial';
  @Input() experiments: Experiment[] = [];
  @Output() getExport = new EventEmitter<number[]>();

  selectedIds = new Set<number>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['experiments']) {
      this.cleanupSelection();
    }
  }

  private cleanupSelection(): void {
    if (!this.experiments || this.experiments.length === 0) {
      this.selectedIds.clear();
      return;
    }

    const visibleIds = new Set(this.experiments.map(exp => exp.id));

    for (const id of this.selectedIds) {
      if (!visibleIds.has(id)) {
        this.selectedIds.delete(id);
      }
    }
  }

  toggleSelection(expId: number) {
    if (this.selectedIds.has(expId)) {
      this.selectedIds.delete(expId);
    } else {
      this.selectedIds.add(expId);
    }
  }

  isSelected(expId: number): boolean {
    return this.selectedIds.has(expId);
  }

  get allSelected(): boolean {
    if (this.experiments.length === 0) return false;
    return this.experiments.every(exp => this.selectedIds.has(exp.id));
  }

  toggleSelectAll() {
    if (this.allSelected) {
      this.experiments.forEach(exp => this.selectedIds.delete(exp.id));
    } else {
      this.experiments.forEach(exp => this.selectedIds.add(exp.id));
    }
  }

  onExportClicked() {
    this.getExport.emit(Array.from(this.selectedIds));
  }
}
