import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { AppModalComponent } from '../../../../shared/modal/app-modal.component';
import { ReactantStructureComponent } from '../reactant-structure/reactant-structure.component';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { StructureDto } from '../../reactant.dto';

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

  parseFile(structure: StructureDto): File | null {
    if (structure.structure && structure.name) {
      const fileBits = [structure.structure];

      const options = {
        type: 'text/plain;charset=utf-8',
        lastModified: Date.now(),
      };

      return new File(fileBits, structure.name, options);
    }
    return null;
  }

  selectStructure(structure: StructureDto) {
    this.structureSelected.emit(structure);
  }

  rejectStructures() {
    this.structureSelected.emit(null);
  }
}
