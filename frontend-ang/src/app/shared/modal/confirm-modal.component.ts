import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppModalComponent } from './app-modal.component';
import { TRANSLOCO_IMPORTS } from '../../core/global/transloco-imports';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [CommonModule, AppModalComponent, TRANSLOCO_IMPORTS],
  templateUrl: './confirm-modal.component.html',
  styleUrls: ['./confirm-modal.component.scss'],
})
export class AppConfirmModalComponent {
  @Input() title: string = 'shared.modals.confirm.title';
  @Input() message: string = 'shared.modals.confirm.message';
  @Input() confirmLabel: string = 'shared.modals.confirm.confirm';
  @Input() cancelLabel: string = 'shared.modals.confirm.cancel';
  @Input() confirmButtonClass: 'primary' | 'danger' | 'restore' = 'primary';
  @Input() isOpen: boolean = false;
  @Input() icon: string = 'bi-exclamation-triangle-fill';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onConfirm() {
    this.confirm.emit();
  }

  onCancel() {
    this.cancel.emit();
  }
}
