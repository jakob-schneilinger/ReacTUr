import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DragAndDropComponent } from './drag-and-drop/drag-and-drop.component';
import { PasteTableComponent } from './paste-table/paste-table.component';
import { UploadedFileComponent } from './uploaded-file/uploaded-file.component';
import { AppConfirmModalComponent } from '../modal/confirm-modal.component';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '../../core/layout/notification/notification.service';
import { TRANSLOCO_IMPORTS } from '../../core/global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DragAndDropComponent,
    PasteTableComponent,
    UploadedFileComponent,
    AppConfirmModalComponent,
    TRANSLOCO_IMPORTS,
  ],
})
export class FileUploadComponent implements OnInit, OnChanges {
  @Input() allowedExtensions: string[] = [];
  @Input() showTable: boolean = true;
  @Input() existingFiles?: File[];
  @Input() maxFileSizeMB?: number;
  @Input() maxFiles: number = 0;
  @Input() removePossible: boolean = true;

  @Output() filesChanged = new EventEmitter<File[]>();

  constructor(
    private route: ActivatedRoute,
    private notification: NotificationService,
    private transloco: TranslocoService,
  ) {}

  files: File[] = [];

  isDeleteFileModalOpen: boolean = false;
  pendingDeleteIndex: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['existingFiles'] && changes['existingFiles'].currentValue) {
      this.files = [...changes['existingFiles'].currentValue];
    }
  }

  ngOnInit(): void {}

  onPasteTableGenerated(file: File) {
    if (this.maxFileSizeMB) {
      const maxSizeBytes = this.maxFileSizeMB * 1024 * 1024;
      if (file.size > maxSizeBytes) {
        this.notification.error(
          this.transloco.translate('shared.upload.dragDrop.errors.uploadError'),
          this.transloco.translate('shared.upload.pastedDataExceedsSize', { size: this.maxFileSizeMB }),
        );
        return;
      }
    }

    this.addFiles([file]);
  }

  addFiles(newFiles: FileList | File[]) {
    const filesArray = Array.from(newFiles);

    if (this.maxFiles > 0 && this.files.length + filesArray.length > this.maxFiles) {
      this.notification.error(
        this.transloco.translate('shared.upload.dragDrop.errors.uploadError'),
        this.transloco.translate('shared.upload.maxFilesReached', { count: this.maxFiles }),
      );
      return;
    }

    this.files.push(...filesArray);
    this.filesChanged.emit(this.files);

    if (filesArray.length === 1) {
      this.notification.success(
        this.transloco.translate('shared.upload.fileAdded'),
        this.transloco.translate('shared.upload.fileAddedMessage', { fileName: filesArray[0].name }),
      );
    } else {
      this.notification.success(
        this.transloco.translate('shared.upload.filesAdded'),
        this.transloco.translate('shared.upload.filesAddedMessage', { count: filesArray.length }),
      );
    }
  }

  get pendingFileName(): string {
    return this.pendingDeleteIndex !== null ? this.files[this.pendingDeleteIndex]?.name : '';
  }

  confirmRemoveFile(index: number) {
    this.pendingDeleteIndex = index;
    this.isDeleteFileModalOpen = true;
  }

  cancelDelete() {
    this.isDeleteFileModalOpen = false;
    this.pendingDeleteIndex = null;
  }

  executeDelete() {
    if (this.pendingDeleteIndex !== null) {
      const tempName = this.files[this.pendingDeleteIndex]?.name;
      this.files.splice(this.pendingDeleteIndex, 1);

      this.filesChanged.emit(this.files);

      this.notification.success(
        this.transloco.translate('shared.upload.fileRemoved'),
        this.transloco.translate('shared.upload.fileRemovedMessage', { fileName: tempName }),
      );
    }
    this.cancelDelete();
  }
}
