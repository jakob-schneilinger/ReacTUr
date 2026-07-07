import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../core/layout/notification/notification.service';
import { TRANSLOCO_IMPORTS } from '../../../core/global/transloco-imports';
import { DragAndDropDirective } from '../../directives/drag-and-drop.directive';
import { TranslocoService } from '@jsverse/transloco';

@Component({
  selector: 'app-drag-and-drop',
  templateUrl: './drag-and-drop.component.html',
  styleUrls: ['./drag-and-drop.component.scss'],
  standalone: true,
  imports: [CommonModule, DragAndDropDirective, TRANSLOCO_IMPORTS],
})
export class DragAndDropComponent {
  @Output() filesSelected = new EventEmitter<File[]>();
  @Input() allowedExtensions: string[] = [];
  @Input() maxFileSizeMB?: number;
  @Input() maxFiles: number = 0;

  constructor(
    private notification: NotificationService,
    private transloco: TranslocoService,
  ) {}

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
        const formatter = new Intl.ListFormat(this.transloco.getActiveLang(), {
          style: 'long',
          type: 'conjunction',
        });
        this.onFileError(
          this.transloco.translate('shared.upload.dragDrop.errors.typeNotAllowed', {
            extension,
            allowed: formatter.format(dottedExtensions),
          }),
        );
        continue;
      }

      if (this.maxFileSizeMB) {
        const maxSizeBytes = this.maxFileSizeMB * 1024 * 1024;
        if (file.size > maxSizeBytes) {
          this.onFileError(
            this.transloco.translate('shared.upload.dragDrop.errors.sizeExceeded', {
              fileName: file.name,
              size: this.maxFileSizeMB,
            }),
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
    this.notification.error(
      this.transloco.translate('shared.upload.dragDrop.errors.uploadError'),
      errorMessage,
    );
  }

  get acceptedExtensionsString(): string {
    return this.allowedExtensions.map((ext) => `.${ext}`).join(',');
  }

  get acceptedExtensionsText(): string {
    if (this.allowedExtensions.length === 0)
      return this.transloco.translate('shared.upload.dragDrop.allFiles');
    return this.allowedExtensions
      .map((ext, index): string => {
        if (this.allowedExtensions.length <= 1) return `${ext.toUpperCase()}`;
        if (this.allowedExtensions.length - 1 == index)
          return `${this.transloco.translate('shared.upload.dragDrop.and')} ${ext.toUpperCase()}`;
        return `${ext.toUpperCase()}`;
      })
      .join(', ');
  }
}
