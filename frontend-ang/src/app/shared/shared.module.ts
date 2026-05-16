import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FileUploadComponent } from '../components/file-upload/file-upload.component';
import { UploadedFileComponent } from '../components/file-upload/uploaded-file/uploaded-file.component';
import { DragAndDropComponent } from '../components/file-upload/drag-and-drop/drag-and-drop.component';
import { PasteTableComponent } from '../components/file-upload/paste-table/paste-table.component';
import { DragAndDropDirective } from '../directives/drag-and-drop.directive';
import { NotificationsComponent } from '../components/notification/notifications.component';
import { LayoutComponent } from '../components/layout/layout.component';

@NgModule({
  declarations: [
    FileUploadComponent,
    UploadedFileComponent,
    DragAndDropComponent,
    PasteTableComponent,
    NotificationsComponent,
    LayoutComponent,
  ],
  imports: [CommonModule, DragAndDropDirective],
  exports: [
    FileUploadComponent,
    UploadedFileComponent,
    DragAndDropComponent,
    PasteTableComponent,
    NotificationsComponent,
    LayoutComponent,
    DragAndDropDirective,
    CommonModule,
  ],
})
export class SharedModule {}
