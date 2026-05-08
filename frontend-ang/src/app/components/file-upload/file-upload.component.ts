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

  addFiles(newFiles: FileList | File[]) {
    // Convert FileList to Array and append to existing files
    const filesArray = Array.from(newFiles);
    this.files.push(...filesArray);
  }

  removeFile(index: number) {
    this.files.splice(index, 1);
  }

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
}
