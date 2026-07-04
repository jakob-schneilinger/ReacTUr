import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { AppModalComponent } from '../../../../shared/modal/app-modal.component';
import { ReactantStructureComponent } from '../reactant-structure/reactant-structure.component';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { StructureDto } from '../../reactant.dto';
import { StructureService } from '../../structure.service';

@Component({
  selector: 'app-reactant-structure-confirm-modal',
  templateUrl: './reactant-structure-confirm-modal.component.html',
  styleUrls: ['./reactant-structure-confirm-modal.component.scss'],
  standalone: true,
  imports: [CommonModule, AppModalComponent, ReactantStructureComponent, TRANSLOCO_IMPORTS],
})
export class ReactantStructureConfirmModalComponent {  @Input() title: string = 'Confirm';
  @Input() structure!: File;
  @Input() baseSmiles: string = '';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onConfirm() {
    this.confirm.emit();
  }

  onCancel() {
    this.cancel.emit();
  }
}
