import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { DragAndDropDirective } from '../../../directives/drag-and-drop.directive';

@Component({
  selector: 'app-drag-and-drop',
  templateUrl: './drag-and-drop.component.html',
  styleUrls: ['./drag-and-drop.component.scss'],
  standalone: false,
})
export class DragAndDropComponent {
  @Output() filesSelected = new EventEmitter<FileList | File[]>();

  onFileDropped(files: FileList) {
    this.filesSelected.emit(files);
  }

  fileBrowseHandler(event: any) {
    const files = event.target.files;
    this.filesSelected.emit(files);
    event.target.value = null;
  }
}
