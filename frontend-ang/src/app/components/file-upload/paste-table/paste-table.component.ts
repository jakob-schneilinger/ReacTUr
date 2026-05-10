import { Component, EventEmitter, OnInit, Output } from '@angular/core';

@Component({
  selector: 'app-paste-table',
  templateUrl: './paste-table.component.html',
  styleUrls: ['./paste-table.component.scss'],
  standalone: false,
})
export class PasteTableComponent {
  @Output() fileGenerated = new EventEmitter<File>();

  tableData: string[][] = [];

  onPaste(event: ClipboardEvent): void {
    const clipboardData = event.clipboardData;
    const pastedText = clipboardData?.getData('text');

    if (pastedText) {
      event.preventDefault();
      this.parsePastedData(pastedText);
    }
  }

  private parsePastedData(text: string): void {
    const rows = text.split('\n');

    this.tableData = rows.map(row => row.split('\t'));

    this.tableData = this.tableData.filter(row =>
      row.some(cell => cell && cell.trim() !== '')
    );
  }

  clearTable(): void {
    this.tableData = [];
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

  private getTimestamp():string{
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
