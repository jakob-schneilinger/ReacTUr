import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { Experiment } from '../../dtos/experiment';

@Component({
  selector: 'app-export-modal',
  templateUrl: './export-modal.component.html',
  styleUrls: ['./export-modal.component.scss'],
  standalone: false
})
export class ExportModalComponent implements OnInit {
  @Input() selectedExperiments: Experiment[] = [];
  @Output() close = new EventEmitter<void>();

  exportFormat: 'json' | 'csv' = 'json';

  exportFields = {
    core: {
      reactants: true,
      date: true,
      scientist: true,
      temperature: true,
      solvent: true
    },
    additional: {
      instrument: false,
      modality: false,
      wavelength: false,
      comment: false
    }
  };

  previewData: string = '';

  ngOnInit() {
    this.updatePreview();
  }

  toggleFormat(format: 'json' | 'csv') {
    this.exportFormat = format;
    this.updatePreview();
  }

  onFieldChange() {
    this.updatePreview();
  }

  updatePreview() {
    const previewItems = this.selectedExperiments.slice(0, 10);

    if (this.exportFormat === 'json') {
      this.previewData = JSON.stringify(previewItems, null, 2);
    } else {
      this.previewData = "ID, Reactants, Date, Scientist, Temperature\n";
      previewItems.forEach(exp => {
        this.previewData += `${exp.id}, "${exp.reactants[0].shortName} + ${exp.reactants[1].shortName}", ${exp.metadata.date.toLocaleDateString()}, ${exp.metadata.scientist}, ${exp.metadata.temperature}\n`;
      });
    }
  }

  downloadFile() {
    console.log(`Downloading ${this.selectedExperiments.length} items as ${this.exportFormat.toUpperCase()}`);
    this.close.emit();
  }
}
