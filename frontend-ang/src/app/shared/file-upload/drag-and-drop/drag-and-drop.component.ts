import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../core/layout/notification/notification.service';
import { DragAndDropDirective } from '../../directives/drag-and-drop.directive';

@Component({
  selector: 'app-drag-and-drop',
  templateUrl: './drag-and-drop.component.html',
  styleUrls: ['./drag-and-drop.component.scss'],
  standalone: true,
  imports: [CommonModule, DragAndDropDirective],
})
export class DragAndDropComponent {
  @Output() filesSelected = new EventEmitter<File[]>();
  @Input() allowedExtensions: string[] = [];
  @Input() maxFileSizeMB?: number;
  @Input() maxFiles: number = 0;

  constructor(private notification: NotificationService) {}

  onFileDropped(files: FileList | File[]) {
    this.processFiles(files);
  }

  fileBrowseHandler(event: any) {
    const files = event.target.files;
    if (files && files.length > 0) {
      this.processFiles(files);
    }
    event.target.value = null;
  }

  private processFiles(files: FileList | File[]) {
    const validFiles: File[] = [];
    const fileArray = Array.from(files);

    for (let i = 0; i < fileArray.length; i++) {
      const file = fileArray[i];
      const extension = file.name.split('.').pop()?.toLowerCase();

      if (!extension || !this.allowedExtensions.includes(extension)) {
        const dottedExtensions = this.allowedExtensions.map((ext) => `.${ext}`);
        const formatter = new Intl.ListFormat('en', { style: 'long', type: 'conjunction' });
        this.onFileError(
          `File type .${extension} is not allowed. Only ${formatter.format(dottedExtensions)} are supported.`,
        );
        continue;
      }

      if (this.maxFileSizeMB) {
        const maxSizeBytes = this.maxFileSizeMB * 1024 * 1024;
        if (file.size > maxSizeBytes) {
          this.onFileError(
            `File "${file.name}" exceeds the maximum size of ${this.maxFileSizeMB}MB.`,
          );
          continue;
        }
      }

      validFiles.push(file);
    }

    if (validFiles.length > 0) {
      this.filesSelected.emit(validFiles);
    }
  }

  onFileError(errorMessage: string) {
    this.notification.error('Upload Error', errorMessage);
  }

  get acceptedExtensionsString(): string {
    return this.allowedExtensions.map((ext) => `.${ext}`).join(',');
  }

  get acceptedExtensionsText(): string {
    if (this.allowedExtensions.length === 0) return 'All files';
    return this.allowedExtensions
      .map((ext, index): string => {
        if (this.allowedExtensions.length <= 1) return `${ext.toUpperCase()}`;
        if (this.allowedExtensions.length - 1 == index) return `and ${ext.toUpperCase()}`;
        return `${ext.toUpperCase()}`;
      })
      .join(', ');
  }
}
