import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppModalComponent } from '../../../../shared/modal/app-modal.component';

// Interface to track the state of each row
interface MetadataItem {
  key: string;
  value: any;
  selected: boolean;
}

@Component({
  selector: 'app-extracted-metadata-confirm-modal',
  templateUrl: './extracted-metadata-confirm-modal.component.html',
  styleUrls: ['./extracted-metadata-confirm-modal.component.scss'],
  standalone: true,
  imports: [CommonModule, AppModalComponent],
})
export class ExtractedMetadataConfirmModalComponent implements OnChanges {
  @Input() metadata: Record<string, any> | null = null;
  @Input() fileName: string = '';

  // custom sort order
  @Input() sortOrder: string[] = [
    'reactantA',
    'reactantB',
    'concentrationA',
    'concentrationB',
    'temperature',
    'wavelength',
    'date',
    'instrument',
    'modality',
    'comment',
  ];

  @Output() confirm = new EventEmitter<Record<string, any>>();
  @Output() cancel = new EventEmitter<void>();

  displayData: MetadataItem[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['metadata'] && this.metadata) {
      this.processMetadata(this.metadata);
    }
  }

  private processMetadata(data: Record<string, any>): void {
    const items: MetadataItem[] = Object.keys(data).map((key) => {
      let processedValue = data[key];

      // Converts "yyyy/mm/dd" -> "yyyy-mm-dd"
      if (key === 'date' && typeof processedValue === 'string') {
        processedValue = processedValue.replace(/\//g, '-');
      }

      return {
        key,
        value: processedValue,
        selected: true,
      };
    });

    items.sort((a, b) => {
      const indexA = this.sortOrder.indexOf(a.key);
      const indexB = this.sortOrder.indexOf(b.key);

      if (indexA !== -1 && indexB !== -1) return indexA - indexB;
      if (indexA !== -1) return -1;
      if (indexB !== -1) return 1;
      return a.key.localeCompare(b.key);
    });

    this.displayData = items;
  }

  toggleSelection(item: MetadataItem): void {
    item.selected = !item.selected;
  }

  onConfirm(): void {
    const selectedMetadata = this.displayData
      .filter((item) => item.selected)
      .reduce(
        (acc, item) => {
          acc[item.key] = item.value;
          return acc;
        },
        {} as Record<string, any>,
      );

    this.confirm.emit(selectedMetadata);
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
