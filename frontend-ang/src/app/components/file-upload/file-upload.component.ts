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
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { ExperimentService } from '../../services/experiment.service';

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss'],
  standalone: false,
})
export class FileUploadComponent implements OnInit, OnChanges {
  @Input() allowedExtensions: string[] = [];
  @Input() showTable: boolean = true;
  @Input() existingFiles?: File[];

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
    this.addFiles([file]);
    this.notification.success('File Added', `Pasted data converted and added as "${file.name}".`);
  }

  addFiles(newFiles: FileList | File[]) {
    const filesArray = Array.from(newFiles);
    this.files.push(...filesArray);

    this.filesChanged.emit(this.files);
  }

  removeFile(index: number) {
    this.files.splice(index, 1);

    this.filesChanged.emit(this.files);
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
