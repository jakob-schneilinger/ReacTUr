import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-paste-table',
  standalone: true, // Assuming you are using standalone components, remove if using NgModules
  imports: [CommonModule],
  templateUrl: './paste-table.component.html',
  styleUrls: ['./paste-table.component.scss'],
})
export class PasteTableComponent {
  tableData: string[][] = [];

  // Listen for paste events on this specific component
  onPaste(event: ClipboardEvent): void {
    const clipboardData = event.clipboardData;
    const pastedText = clipboardData?.getData('text');

    if (pastedText) {
      // Prevent default to avoid pasting into inputs if any are focused
      event.preventDefault();
      this.parsePastedData(pastedText);
    }
  }

  private parsePastedData(text: string): void {
    // Excel/Sheets separate columns by Tab (\t) and rows by Newline (\n)
    const rows = text.split('\n');

    this.tableData = rows.map((row) => row.split('\t'));

    // Filter out completely empty rows
    this.tableData = this.tableData.filter((row) => row.some((cell) => cell && cell.trim() !== ''));
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

            // Escape quotes and wrap in quotes if the cell contains a comma, quote, or newline
            if (cellString.includes(',') || cellString.includes('"') || cellString.includes('\n')) {
              cellString = `"${cellString.replace(/"/g, '""')}"`;
            }
            return cellString;
          })
          .join(',');
      })
      .join('\n');

    // Create a Blob and trigger download
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.setAttribute('href', url);
    link.setAttribute('download', 'pasted_data.csv');
    link.style.visibility = 'hidden';

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
}
