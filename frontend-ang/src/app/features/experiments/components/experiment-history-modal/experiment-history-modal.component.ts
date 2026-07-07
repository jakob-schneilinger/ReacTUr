import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ExperimentHistoryDto } from '../../experiment-history.dto';
import { AppModalComponent } from '../../../../shared/modal/app-modal.component';
import { CommonModule } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-experiment-history-modal',
  templateUrl: './experiment-history-modal.component.html',
  styleUrls: ['./experiment-history-modal.component.scss'],
  standalone: true,
  imports: [CommonModule, AppModalComponent, TranslocoModule],
})
export class ExperimentHistoryModalComponent {
  @Input() isOpen: boolean = false;
  @Input() history: ExperimentHistoryDto[] = [];

  @Output() close = new EventEmitter<void>();

  private readonly ORDER = [
    'reactantA', 'reactantB', 'date', 'scientist', 'temperature', 'solvent',
    'concentrationA', 'concentrationB', 'rateConstant', 'errorValue',
    'isErrorPercentage', 'pfoRateConstant', 'instrument', 'modality',
    'wavelength', 'dois', 'comment', 'createdBy'
  ];

  private readonly TRANSLATION_MAPPING: Record<string, string> = {
    'concentrationA': 'concentrationReactantA',
    'concentrationB': 'concentrationReactantB',
    'errorValue': 'error'
  };

  onClose() {
    this.close.emit();
  }

  getChangeKeys(changes: Record<string, string>): string[] {
    const rawKeys = Object.keys(changes);

    return rawKeys.sort((a, b) => {
      const indexA = this.ORDER.indexOf(a);
      const indexB = this.ORDER.indexOf(b);

      if (indexA !== -1 && indexB !== -1) return indexA - indexB;
      if (indexA !== -1) return -1;
      if (indexB !== -1) return 1;
      return a.localeCompare(b);
    });
  }

  splitChange(changeString: string): { oldVal: string, newVal: string } {
    const parts = changeString.split(' -> ');
    return {
      oldVal: parts[0] === 'null' ? ' ' : parts[0],
      newVal: parts[1] === 'null' ? ' ' : parts[1]
    };
  }

  getTranslationKey(backendKey: string): string {
    const mappedKey = this.TRANSLATION_MAPPING[backendKey] || backendKey;
    return `experiments.details.form.labels.${mappedKey}`;
  }
}
