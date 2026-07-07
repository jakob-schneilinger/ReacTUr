import { ChangeDetectorRef, Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TRANSLOCO_IMPORTS } from '../../../core/global/transloco-imports';

@Component({
  selector: 'app-paste-table',
  templateUrl: './paste-table.component.html',
  styleUrls: ['./paste-table.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, TRANSLOCO_IMPORTS],
})
export class PasteTableComponent {
  @Output() fileGenerated = new EventEmitter<File>();

  tableData: string[][] = [];
  previewData: string[][] = [];
  isLoading: boolean = false;

  readonly MAX_PREVIEW_ROWS = 100;

  constructor(
    private cdr: ChangeDetectorRef,
  ) {
  }


  onPaste(event: ClipboardEvent): void {
    const clipboardData = event.clipboardData;
    const pastedText = clipboardData?.getData('text');

    if (pastedText) {
      event.preventDefault();
      this.isLoading = true;
      this.cdr.detectChanges();

      setTimeout(() => {
        try {
          this.parsePastedData(pastedText);
        } finally {
          this.isLoading = false;
          this.cdr.markForCheck();
        }
      }, 50);
    }
  }

  private parsePastedData(text: string): void {
    const separator = this.detectSeparator(text);

    const rawRows = text.split(/\r?\n/);

    this.tableData = rawRows
      .map((row) => this.parseRow(row, separator))
      .filter((row) => row.some((cell) => cell && cell.trim() !== ''));

    // normalise numbers
    this.tableData = this.tableData.map((row) => row.map((cell) => this.normalizeNumber(cell)));

    this.previewData = this.tableData.slice(0, this.MAX_PREVIEW_ROWS);
  }

  private detectSeparator(text: string): string {
    const firstLine = text.split('\n')[0] || '';
    if (firstLine.includes('\t')) return '\t'; // tab
    if (firstLine.includes(';')) return ';'; // ;
    if (firstLine.includes(',')) return ','; // ,
    return '\t'; // Fallback
  }

  private parseRow(row: string, separator: string): string[] {
    const result: string[] = [];
    let currentCell = '';
    let inQuotes = false;

    for (let i = 0; i < row.length; i++) {
      const char = row[i];
      if (char === '"') {
        inQuotes = !inQuotes;
      } else if (char === separator && !inQuotes) {
        result.push(currentCell.trim());
        currentCell = '';
      } else {
        currentCell += char;
      }
    }
    result.push(currentCell.trim());

    return result.map((cell) => cell.replace(/^"|"$/g, '').replace(/""/g, '"'));
  }

  private normalizeNumber(value: string): string {
    const trimmed = value.trim();

    if (!/^[-]?[\d.,]+$/.test(trimmed)) {
      return value;
    }

    const lastDot = trimmed.lastIndexOf('.');
    const lastComma = trimmed.lastIndexOf(',');

    // remove thousand point and change comma to decimal point (1.234,56 to 1234.56)
    if (lastComma > lastDot) {
      return trimmed.replace(/\./g, '').replace(',', '.');
    }

    // remove thousand comma (1,234.56 to 1234.56)
    if (lastDot > lastComma && lastComma !== -1) {
      return trimmed.replace(/,/g, '');
    }

    return trimmed;
  }

  clearTable(): void {
    this.tableData = [];
    this.previewData = [];
  }

  saveAsCsv(): void {
    if (this.tableData.length === 0) return;

    const csvContent = this.tableData
      .map((row) => {
        return row
          .map((cell) => {
            let cellString = cell === undefined || cell === null ? '' : String(cell).trim();

            if (cellString.includes(',') || cellString.includes('"') || cellString.includes('\n')) {
              cellString = `"${cellString.replace(/"/g, '""')}"`;
            }
            return cellString;
          })
          .join(';');
      })
      .join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });

    const fileName = `pasted_data_${this.getTimestamp()}.csv`;
    const file = new File([blob], fileName, { type: 'text/csv' });

    this.fileGenerated.emit(file);

    //TODO: add if data should be removed from table
    //this.clearTable();
  }

  private getTimestamp(): string {
    const date = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    const seconds = pad(date.getSeconds());

    return year + month + day + hours + minutes + seconds;
  }
}
