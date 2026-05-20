import { Component, EventEmitter, Input, Output } from '@angular/core';


@Component({
  selector: 'app-uploaded-file',
  templateUrl: './uploaded-file.component.html',
  styleUrls: ['./uploaded-file.component.scss'],
  standalone: false,
})
export class UploadedFileComponent {
  @Input() file!: File;
  @Input() removePossible: boolean = true;
  @Output() remove = new EventEmitter<void>();
  @Output() download = new EventEmitter<void>();

  getExtension(filename: string): string {
    return filename.split('.').pop() || 'File';
  }

  onRemove() {
    this.remove.emit();
  }

  onDownload() {
    const url = window.URL.createObjectURL(this.file);
    const a = document.createElement('a');

    a.href = url;
    a.download = this.file.name;

    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    window.URL.revokeObjectURL(url);
    this.download.emit();
  }
}
