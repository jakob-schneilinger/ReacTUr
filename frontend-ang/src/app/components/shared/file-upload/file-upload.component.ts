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
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';
import { ExperimentService } from '../../../services/experiment.service';

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
    AppConfirmModalComponent
  ],
})
export class FileUploadComponent implements OnInit, OnChanges {
  @Input() allowedExtensions: string[] = [];
  @Input() showTable: boolean = true;
  @Input() existingFiles?: File[];
  @Input() maxFileSizeMB?: number;
  @Input() maxFiles: number = 0;

  @Output() filesChanged = new EventEmitter<File[]>();

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
    private experiments: ExperimentService,
  ) {}

  files: File[] = [];

  isDeleteFileModalOpen: boolean = false;
  pendingDeleteIndex: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['existingFiles'] && changes['existingFiles'].currentValue) {
      this.files = [...changes['existingFiles'].currentValue];
    }
  }

  ngOnInit(): void {
    this.route.params.subscribe({
      next: (params) => {
        const id = params['id'];
        if (!id) {
          //TODO: Get already uploaded files
          //this.experiments.getFiles();
        }
      },
    });
  }

  onPasteTableGenerated(file: File) {
    if (this.maxFileSizeMB) {
      const maxSizeBytes = this.maxFileSizeMB * 1024 * 1024;
      if (file.size > maxSizeBytes) {
        this.notification.error(
          'Upload Error',
          `Pasted data exceeds the maximum allowed file size of ${this.maxFileSizeMB}MB.`,
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
        'Upload Error',
        `Maximum number of files reached. Only ${this.maxFiles} file(s) are allowed.`,
      );
      return;
    }

    this.files.push(...filesArray);
    this.filesChanged.emit(this.files);

    if (filesArray.length === 1) {
      this.notification.success('File Added', `"${filesArray[0].name}" has been added.`);
    } else {
      this.notification.success('Success', `${filesArray.length} files added.`);
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
        'File Removed',
        `The file "${tempName}" was successfully removed from the list.`,
      );
    }
    this.cancelDelete();
  }
}
