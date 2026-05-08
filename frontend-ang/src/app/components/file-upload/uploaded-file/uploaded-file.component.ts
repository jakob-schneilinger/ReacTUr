import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-uploaded-file',
  templateUrl: './uploaded-file.component.html',
  styleUrls: ['./uploaded-file.component.scss'],
  standalone: false,
})
export class UploadedFileComponent {
  @Input() file!: File;
  @Output() remove = new EventEmitter<void>();
  @Output() download = new EventEmitter<void>();

  getExtension(filename: string): string {
    return filename.split('.').pop() || 'File';
  }

  onRemove() {
    this.remove.emit();
  }

  onDownload() {
    this.download.emit();
    // Actual download logic would typically be handled here or in the parent via URL.createObjectURL
  }
}
