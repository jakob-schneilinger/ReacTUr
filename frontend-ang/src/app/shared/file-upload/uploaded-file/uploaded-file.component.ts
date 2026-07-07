import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import ExperimentFileService from '../../../features/experiments/experiment-file.service';

import { TRANSLOCO_IMPORTS } from '../../../core/global/transloco-imports';

@Component({
  selector: 'app-uploaded-file',
  templateUrl: './uploaded-file.component.html',
  styleUrls: ['./uploaded-file.component.scss'],
  standalone: true,
  imports: [CommonModule, TRANSLOCO_IMPORTS],
})
export class UploadedFileComponent {
  @Input() file!: File;
  @Input() removePossible: boolean = true;
  @Output() remove = new EventEmitter<void>();
  @Output() download = new EventEmitter<void>();

  constructor(private experimentFileService: ExperimentFileService) {
  }

  get isFakeFile(): boolean {
    return !!(this.file as any).isFake;
  }

  getExtension(filename: string): string {
    return filename.split('.').pop() || 'shared.file';
  }

  onRemove() {
    this.remove.emit();
  }

  onDownload() {
    const customFile = this.file as any;

    if (customFile.isFake && customFile.urlId) {

      this.experimentFileService.downloadFile(customFile.urlId).subscribe({
        next: value => {
          const url = window.URL.createObjectURL(value);
          const a = document.createElement('a');

          a.href = url;
          a.download = this.file.name;

          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);

          window.URL.revokeObjectURL(url);
          this.download.emit();
        }
      });
    } else {
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
}
