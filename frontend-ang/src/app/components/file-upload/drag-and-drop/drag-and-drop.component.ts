import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-drag-and-drop',
  templateUrl: './drag-and-drop.component.html',
  styleUrls: ['./drag-and-drop.component.scss'],
  standalone: false,
})
export class DragAndDropComponent {
  @Output() filesSelected = new EventEmitter<FileList | File[]>();
  @Input() allowedExtensions: string[] = [];

  constructor(private notificationService: NotificationService) {}

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

      if (extension && this.allowedExtensions.includes(extension)) {
        validFiles.push(file);
      } else {
        const dottedExtensions = this.allowedExtensions.map((ext) => `.${ext}`);
        const formatter = new Intl.ListFormat('en', { style: 'long', type: 'conjunction' });
        const allowedExtensionsText = formatter.format(dottedExtensions);

        this.onFileError(
          `File type .${extension} is not allowed. Only ${allowedExtensionsText} are supported.`,
        );
      }
    }

    if (validFiles.length > 0) {
      this.filesSelected.emit(validFiles);
      this.notificationService.success('Success', `${validFiles.length} file(s) added.`);
    }
  }

  onFileError(errorMessage: string) {
    this.notificationService.error('Upload Error', errorMessage);
  }

  get acceptedExtensionsString(): string {
    return this.allowedExtensions.map((ext) => `.${ext}`).join(',');
  }

  get acceptedExtensionsText(): string {
    return this.allowedExtensions
      .map((ext, index): string => {
        if (this.allowedExtensions.length <= 1) {
          return `${ext.toUpperCase()}`;
        }
        if (this.allowedExtensions.length - 1 == index) {
          return `and ${ext.toUpperCase()}`;
        }
        return `${ext.toUpperCase()}`;
      })
      .join(', ');
  }

}
