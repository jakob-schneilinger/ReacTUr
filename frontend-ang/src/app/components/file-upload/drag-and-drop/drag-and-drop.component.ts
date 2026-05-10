import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-drag-and-drop',
  templateUrl: './drag-and-drop.component.html',
  styleUrls: ['./drag-and-drop.component.scss'],
  standalone: false,
})
export class DragAndDropComponent {
  @Output() filesSelected = new EventEmitter<FileList | File[]>();

  private allowedExtensions = ['txt', 'csv', 'dsx'];

  constructor(private notificationService: NotificationService) {}

  onFileDropped(files: FileList | File[]) {
    this.filesSelected.emit(files);
    this.notificationService.success('Success', `${files.length} file(s) added.`);
  }

  onFileError(errorMessage: string) {
    this.notificationService.error('Upload Error', errorMessage);
  }

  fileBrowseHandler(event: any) {
    const files = event.target.files;
    const validFiles: File[] = [];

    if (files.length > 0) {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const extension = file.name.split('.').pop()?.toLowerCase();

        if (extension && this.allowedExtensions.includes(extension)) {
          validFiles.push(file);
        } else {
          this.onFileError(`File type .${extension} is not allowed. Only .txt, .csv, and .dsx are supported.`);
        }
      }

      if (validFiles.length > 0) {
        this.onFileDropped(validFiles);
      }
    }
    event.target.value = null;
  }
}
