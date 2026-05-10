import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { Experiment } from '../../dtos/experiment';
import { ExperimentService } from '../../services/experiment.service';

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
  isCopied: boolean = false;
  isLoading: boolean = true;

  fullExportData: any[] = [];

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

  constructor(private experimentService: ExperimentService,
  private cd: ChangeDetectorRef,) {}

  ngOnInit() {
    const idsToLoad = this.selectedExperiments.map(exp => exp.id);

    this.experimentService.getBulkExperimentData(idsToLoad).subscribe(heavyData => {

      this.fullExportData = this.selectedExperiments.map(exp => {
        const matchingHeavyData = heavyData.find(hd => hd.experimentId === exp.id);
        return {
          ...exp,
          measurements: matchingHeavyData ? matchingHeavyData.measurements : []
        };
      });
      this.isLoading = false;
      this.updatePreview();
      this.cd.detectChanges();
    });
  }

  toggleFormat(format: 'json' | 'csv') {
    this.exportFormat = format;
    this.updatePreview();
  }

  onFieldChange() {
    this.updatePreview();
  }

  updatePreview() {
    if (this.isLoading) return;

    const rawPreviewItems = this.fullExportData.slice(0, 2);

    const filteredItems = rawPreviewItems.map(exp => {
      const filteredExp: any = { id: exp.id };

      if (this.exportFields.core.reactants) filteredExp.reactants = exp.reactants;

      const meta: any = {};
      if (this.exportFields.core.date) meta.date = exp.metadata.date;
      if (this.exportFields.core.scientist) meta.scientist = exp.metadata.scientist;
      if (this.exportFields.core.temperature) meta.temperature = exp.metadata.temperature;
      if (this.exportFields.core.solvent) meta.solvent = exp.metadata.solvent;

      if (this.exportFields.additional.instrument && exp.metadata.instrument) meta.instrument = exp.metadata.instrument;
      if (this.exportFields.additional.modality && exp.metadata.modality) meta.modality = exp.metadata.modality;
      if (this.exportFields.additional.wavelength && exp.metadata.wavelength) meta.wavelength = exp.metadata.wavelength;
      if (this.exportFields.additional.comment && exp.metadata.comment) meta.comment = exp.metadata.comment;

      if (Object.keys(meta).length > 0) filteredExp.metadata = meta;

      if (exp.measurements && exp.measurements.length > 0) {
        filteredExp.measurements = exp.measurements.map((m: any) => ({
          name: m.name,
          xLabel: m.xLabel,
          yLabels: m.yLabels,
          x: m.x.slice(0, 3),
          y: m.y.map((yCol: any[]) => yCol.slice(0, 3)),
          _previewNote: `... (${m.x.length - 3} more datapoints omitted in preview)`
        }));
      }

      return filteredExp;
    });

    if (this.exportFormat === 'json') {
      this.previewData = JSON.stringify(filteredItems, null, 2);
    } else {
      this.previewData = this.generateCSV(filteredItems);
    }
  }

  private generateCSV(items: any[]): string {
    if (items.length === 0) return '';

    const headers = ['ID'];
    if (this.exportFields.core.reactants) headers.push('Reactants');
    if (this.exportFields.core.date) headers.push('Date');
    if (this.exportFields.core.scientist) headers.push('Scientist');
    if (this.exportFields.core.temperature) headers.push('Temp (°C)');
    if (this.exportFields.core.solvent) headers.push('Solvent');
    if (this.exportFields.additional.instrument) headers.push('Instrument');
    if (this.exportFields.additional.modality) headers.push('Modality');
    if (this.exportFields.additional.wavelength) headers.push('Wavelength (nm)');
    if (this.exportFields.additional.comment) headers.push('Comment');

    headers.push('Measurement_Name', 'x', 'y1', 'y2', 'y3', 'y4');

    let csvContent = headers.join(', ') + '\n';

    items.forEach(exp => {
      const baseRow = [exp.id];
      if (this.exportFields.core.reactants) baseRow.push(`"${exp.reactants[0]?.shortName} + ${exp.reactants[1]?.shortName}"`);

      const m = exp.metadata || {};
      if (this.exportFields.core.date) baseRow.push(m.date ? new Date(m.date).toLocaleDateString() : '');
      if (this.exportFields.core.scientist) baseRow.push(m.scientist || '');
      if (this.exportFields.core.temperature) baseRow.push(m.temperature || '');
      if (this.exportFields.core.solvent) baseRow.push(m.solvent || '');
      if (this.exportFields.additional.instrument) baseRow.push(m.instrument || '');
      if (this.exportFields.additional.modality) baseRow.push(m.modality || '');
      if (this.exportFields.additional.wavelength) baseRow.push(m.wavelength || '');
      if (this.exportFields.additional.comment) baseRow.push(`"${m.comment || ''}"`);

      if (exp.measurements && exp.measurements.length > 0) {
        exp.measurements.forEach((measurement: any) => {
          for (let i = 0; i < measurement.x.length; i++) {
            const row = [
              ...baseRow,
              measurement.name,
              measurement.x[i],
              measurement.y[0]?.[i],
              measurement.y[1]?.[i],
              measurement.y[2]?.[i],
              measurement.y[3]?.[i]
            ];
            csvContent += row.join(', ') + '\n';
          }
          if (measurement._previewNote) {
            csvContent += `... (${measurement._previewNote})\n`;
          }
        });
      } else {
        csvContent += [...baseRow, '', '', '', '', '', ''].join(', ') + '\n';
      }
    });

    return csvContent;
  }

  async copyToClipboard() {
    try {
      await navigator.clipboard.writeText(this.previewData);
      this.isCopied = true;
      this.cd.detectChanges();
      setTimeout(() => {
        this.isCopied = false;
      }, 2000);
    } catch (err) {
      console.error('Failed to copy text: ', err);
    }
  }

  downloadFile() {
    console.log(`Downloading ${this.selectedExperiments.length} items as ${this.exportFormat.toUpperCase()}`);
    // TODO: Echten File-Download auslösen
    this.close.emit();
  }
}
