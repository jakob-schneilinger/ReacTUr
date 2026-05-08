import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss'],
  standalone: false,
})
export class FileUploadComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService,
  ) {}

  files: File[] = [];

  isDeleteModalOpen: boolean = false;
  pendingDeleteIndex: number | null = null;

  ngOnInit(): void {
    this.route.params.subscribe({
      next: (params) => {
        const id = params['id'];
        if (!id) {
          this.router.navigate(['/experiments'], { skipLocationChange: true });
          return;
        }
      },
    });
  }

  addFiles(newFiles: FileList | File[]) {
    const filesArray = Array.from(newFiles);
    this.files.push(...filesArray);
  }

  removeFile(index: number) {
    this.files.splice(index, 1);
  }

  get pendingFileName(): string {
    return this.pendingDeleteIndex !== null ? this.files[this.pendingDeleteIndex]?.name : '';
  }

  confirmRemoveFile(index: number) {
    this.pendingDeleteIndex = index;
    this.isDeleteModalOpen = true;
  }

  cancelDelete() {
    this.isDeleteModalOpen = false;
    this.pendingDeleteIndex = null;
  }

  executeDelete() {
    if (this.pendingDeleteIndex !== null) {
      const tempName = this.files[this.pendingDeleteIndex]?.name;
      this.files.splice(this.pendingDeleteIndex, 1);
      this.notification.success(
        'File Removed',
        `The file "${tempName}" was successfully removed from the list.`,
      );
    }
    this.cancelDelete();
  }
}
