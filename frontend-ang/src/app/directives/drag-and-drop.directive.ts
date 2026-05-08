import { Directive, Output, Input, EventEmitter, HostBinding, HostListener } from '@angular/core';

@Directive({
  selector: '[app-drag-and-drop-directive]',
})
export class DragAndDropDirective {
  @HostBinding('class.bg-light') fileOver: boolean = false;
  @Output() fileDropped = new EventEmitter<any>();
  @Output() fileError = new EventEmitter<string>();

  private allowedExtensions = ['txt', 'csv', 'dsx'];

  // Dragover listener
  @HostListener('dragover', ['$event']) onDragOver(evt: any) {
    evt.preventDefault();
    evt.stopPropagation();
    this.fileOver = true;
  }

  // Dragleave listener
  @HostListener('dragleave', ['$event']) public onDragLeave(evt: any) {
    evt.preventDefault();
    evt.stopPropagation();
    this.fileOver = false;
  }

  // Drop listener
  @HostListener('drop', ['$event']) public ondrop(evt: any) {
    evt.preventDefault();
    evt.stopPropagation();
    this.fileOver = false;

    const files: FileList = evt.dataTransfer.files;
    const validFiles: File[] = [];

    if (files.length > 0) {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const extension = file.name.split('.').pop()?.toLowerCase();

        if (extension && this.allowedExtensions.includes(extension)) {
          validFiles.push(file);
        } else {
          this.fileError.emit(
            `File type .${extension} is not allowed. Only .txt, .csv, and .dsx are supported.`,
          );
        }
      }

      if (validFiles.length > 0) {
        this.fileDropped.emit(validFiles);
      }
    }
  }
}
