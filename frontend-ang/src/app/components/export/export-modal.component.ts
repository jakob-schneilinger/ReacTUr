import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { GetExperimentDto } from '../../dtos/experiment.dto';
import { ExperimentService } from '../../services/experiment.service';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';

export interface ExportField {
  id: string;
  label: string;
  category: 'core' | 'additional' | 'data';
}

@Component({
  selector: 'app-export-modal',
  templateUrl: './export-modal.component.html',
  styleUrls: ['./export-modal.component.scss'],
  standalone: false
})
export class ExportModalComponent implements OnInit {
  @Input() selectedExperiments: GetExperimentDto[] = [];
  @Output() close = new EventEmitter<void>();

  exportFormat: 'json' | 'csv' = 'json';
  isCopied: boolean = false;
  isCopying: boolean = false;
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

  availableFields: ExportField[] = [
    { id: 'uuid', label: 'ID', category: 'core' },
    { id: 'reactants', label: 'Reactants', category: 'core' },
    { id: 'date', label: 'Date', category: 'core' },
    { id: 'scientist', label: 'Scientist', category: 'core' },
    { id: 'temperature', label: 'Temperature', category: 'core' },
    { id: 'solvent', label: 'Solvent', category: 'core' },
    { id: 'instrument', label: 'Instrument', category: 'additional' },
    { id: 'measurements', label: 'Measurements (x, y1, y2...)', category: 'data' }
  ];

  selectedColumns: ExportField[] = [];

  drop(event: CdkDragDrop<ExportField[]>) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      const itemToMove = event.previousContainer.data[event.previousIndex];

      if (itemToMove.id === 'measurements') {
        this.availableFields.splice(event.previousIndex, 1);

        this.selectedColumns.splice(event.currentIndex, 0,
          { id: 'x', label: 'Time (x)', category: 'data' },
          { id: 'y1', label: 'y1', category: 'data' },
          { id: 'y2', label: 'y2', category: 'data' },
          { id: 'y3', label: 'y3', category: 'data' }
        );
      } else {
        transferArrayItem(
          event.previousContainer.data,
          event.container.data,
          event.previousIndex,
          event.currentIndex,
        );
      }
    }

    this.updatePreview();
  }

  removeColumn(index: number) {
    const removedItem = this.selectedColumns.splice(index, 1)[0];
    if (!['x', 'y1', 'y2', 'y3'].includes(removedItem.id)) {
      this.availableFields.push(removedItem);
    } else if (removedItem.id === 'x') {
      this.availableFields.push({ id: 'measurements', label: 'Measurements (x, y1, y2...)', category: 'data' });
    }
    this.updatePreview();
  }

  previewData: string = '';
  tableData: any[][] = [];
  csvString: string = '';

  constructor(private experimentService: ExperimentService,
  private cd: ChangeDetectorRef,) {}

  ngOnInit() {
    const idsToLoad = this.selectedExperiments.map(exp => exp.uuid);

    this.experimentService.getBulkExperimentData(idsToLoad).subscribe(heavyData => {

      this.fullExportData = this.selectedExperiments.map((exp) => {
        const matchingHeavyData = heavyData.find((hd) => hd.experimentId === exp.uuid);
        return {
          ...exp,
          measurements: matchingHeavyData ? matchingHeavyData.measurements : [],
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

    if (this.exportFormat === 'json') {
      this.previewData = this.getFormattedJson(true);
    } else {
      this.generateCsvPreview();
    }
  }

  private getFormattedJson(isPreview: boolean): string {
    const itemsToProcess = isPreview ? this.fullExportData.slice(0, 2) : this.fullExportData;

    const filteredItems = itemsToProcess.map(exp => {
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
        filteredExp.measurements = exp.measurements.map((m: any) => {
          const processed: any = {
            name: m.name,
            xLabel: m.xLabel,
            yLabels: m.yLabels,
          };

          if (isPreview) {
            processed.x = m.x.slice(0, 3);
            processed.y = m.y.map((yCol: any[]) => yCol.slice(0, 3));
            processed._previewNote = `... (${m.x.length - 3} more datapoints omitted in preview)`;
          } else {
            processed.x = m.x;
            processed.y = m.y;
          }
          return processed;
        });
      }

      return filteredExp;
    });

    return JSON.stringify(filteredItems, null, 2);
  }

  private getFullCsvString(separator: string = ','): string {
    if (this.selectedColumns.length === 0) return '';
    const allRows = this.buildCsvRows(this.fullExportData);
    const headers = this.selectedColumns.map(col => col.label).join(separator);
    const csvBody = allRows.map(row => row.join(separator)).join('\n');

    return headers + '\n' + csvBody;
  }

  private generateCsvPreview() {
    if (this.selectedColumns.length === 0) {
      this.tableData = [];
      this.csvString = '';
      return;
    }
    const allRows = this.buildCsvRows(this.fullExportData);

    this.tableData = allRows.slice(0, 15);

    const headers = this.selectedColumns.map(col => col.label).join(',');
    const csvBody = allRows.map(row => row.join(',')).join('\n');
    this.csvString = headers + '\n' + csvBody;
  }

  private buildCsvRows(experiments: any[]): any[][] {
    const rows: any[][] = [];

    const hasMeasurements = this.selectedColumns.some(c => ['x', 'y1', 'y2', 'y3'].includes(c.id));

    for (const exp of experiments) {
      if (hasMeasurements && exp.measurements && exp.measurements.length > 0) {
        for (const measurement of exp.measurements) {
          for (let i = 0; i < measurement.x.length; i++) {
            const row = this.selectedColumns.map(col => this.getCellValue(exp, measurement, i, col.id));
            rows.push(row);
          }
        }
      } else {
        const row = this.selectedColumns.map(col => this.getCellValue(exp, null, 0, col.id));
        rows.push(row);
      }
    }

    return rows;
  }

  private getCellValue(exp: any, measurement: any, index: number, colId: string): any {
    switch (colId) {
      case 'id': return exp.id;
      case 'reactants': return exp.reactants ? `"${exp.reactants.map((r: any) => r.shortName).join(' + ')}"` : '';
      case 'date': return exp.metadata?.date ? new Date(exp.metadata.date).toLocaleDateString() : '';
      case 'scientist': return exp.metadata?.scientist || '';
      case 'temperature': return exp.metadata?.temperature || '';
      case 'solvent': return exp.metadata?.solvent || '';
      case 'instrument': return exp.metadata?.instrument || '';

      case 'x': return measurement ? measurement.x[index] : '';
      case 'y1': return measurement?.y[0] ? measurement.y[0][index] : '';
      case 'y2': return measurement?.y[1] ? measurement.y[1][index] : '';
      case 'y3': return measurement?.y[2] ? measurement.y[2][index] : '';

      default: return '';
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
    this.isCopying = true;
    this.cd.detectChanges();
    try {
      let textToCopy = '';

      if (this.exportFormat === 'json') {
        textToCopy = this.getFormattedJson(false);
      } else {
        textToCopy = this.getFullCsvString('\t');
      }

      if (!textToCopy) {
        this.isCopying = false;
        return;
      }

      await navigator.clipboard.writeText(textToCopy);
      this.isCopied = true;
    } catch (err) {
      console.error('Failed to copy text: ', err);
    } finally {
      this.isCopying = false;
      this.cd.detectChanges();

      setTimeout(() => {
        this.isCopied = false;
        this.cd.detectChanges();
      }, 2000);
    }
  }

  downloadFile() {
    let content = '';
    let filename = '';
    let mimeType = '';

    const timestamp = new Date().toISOString().slice(0,10);

    if (this.exportFormat === 'json') {
      content = this.getFormattedJson(false);
      filename = `experiments_${timestamp}.json`;
      mimeType = 'application/json';
    } else {
      content = this.getFullCsvString(',');
      filename = `experiments_${timestamp}.csv`;
      mimeType = 'text/csv;charset=utf-8;';
    }

    const blob = new Blob([content], { type: mimeType });
    const url = window.URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    this.close.emit();
  }
}
