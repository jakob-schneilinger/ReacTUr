import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { AppModalComponent } from '../../../../shared/modal/app-modal.component';
import { ReactantStructureComponent } from '../reactant-structure/reactant-structure.component';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { StructureDto } from '../../reactant.dto';
import { StructureService } from '../../structure.service';

@Component({
  selector: 'app-reactant-structure-select-modal',
  templateUrl: './reactant-structure-select-modal.component.html',
  styleUrls: ['./reactant-structure-select-modal.component.scss'],
  standalone: true,
  imports: [CommonModule, AppModalComponent, ReactantStructureComponent, TRANSLOCO_IMPORTS],
})
export class ReactantStructureSelectModalComponent {
  @Input() pendingStructures: StructureDto[] = [];
  @Input() baseSmiles: string = '';

  @Output() structureSelected = new EventEmitter<StructureDto | null>();

  @Output() close = new EventEmitter<void>();

  constructor(protected structureService: StructureService) {}

  selectStructure(structure: StructureDto) {
    this.structureSelected.emit(structure);
  }

  rejectStructures() {
    this.structureSelected.emit(null);
  }
}
