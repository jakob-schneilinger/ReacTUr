import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDropList, CdkDrag, CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { GetExperimentDto } from '../../../experiments/experiment.dto';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';

export interface ExportField {
  id: string;
  labelKey: string;
  category: 'core' | 'reactants' | 'kinetics' | 'metadata';
}

@Component({
  selector: 'app-export-modal',
  templateUrl: './export-modal.component.html',
  styleUrls: ['./export-modal.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, CdkDropList, CdkDrag, TRANSLOCO_IMPORTS,
  ],
})
export class ExportModalComponent implements OnInit {
  @Input() selectedExperiments: GetExperimentDto[] = [];
  @Output() close = new EventEmitter<void>();

  exportFormat: 'json' | 'csv' = 'json';
  isCopied: boolean = false;
  isCopying: boolean = false;

  exportFields = {
    core: { doi: true, createdBy: true, attachments: false },
    reactants: { names: true, smiles: true, structures: true },
    kinetics: { rateConstant: true, pFO_rateConstant: false, error: true },
    metadata: { temperature: true, solvent: true, instrument: false }
  };

  availableFields: ExportField[] = [
    { id: 'uuid', labelKey: 'exportModal.fields.uuid', category: 'core' },
    { id: 'doi', labelKey: 'exportModal.fields.doi', category: 'core' },
    { id: 'createdBy', labelKey: 'exportModal.fields.createdBy', category: 'core' },
    { id: 'reactantA_name', labelKey: 'exportModal.fields.reactantAName', category: 'reactants' },
    { id: 'reactantA_smiles', labelKey: 'exportModal.fields.reactantASmiles', category: 'reactants' },
    { id: 'reactantA_struct', labelKey: 'exportModal.fields.reactantAStruct', category: 'reactants' },
    { id: 'reactantB_name', labelKey: 'exportModal.fields.reactantBName', category: 'reactants' },
    { id: 'reactantB_smiles', labelKey: 'exportModal.fields.reactantBSmiles', category: 'reactants' },
    { id: 'reactantB_struct', labelKey: 'exportModal.fields.reactantBStruct', category: 'reactants' },
    { id: 'rateConstant', labelKey: 'exportModal.fields.rateConstant', category: 'kinetics' },
    { id: 'pfo', labelKey: 'exportModal.fields.pfo', category: 'kinetics' },
    { id: 'error', labelKey: 'exportModal.fields.error', category: 'kinetics' },
    { id: 'temperature', labelKey: 'exportModal.fields.temperature', category: 'metadata' },
    { id: 'solvent', labelKey: 'exportModal.fields.solvent', category: 'metadata' },
  ];

  selectedColumns: ExportField[] = [];
  previewData: string = '';
  tableData: any[][] = [];

  constructor(
    private cd: ChangeDetectorRef,
    private translocoService: TranslocoService
  ) {}

  ngOnInit() {
    this.updatePreview();
  }

  drop(event: CdkDragDrop<ExportField[]>) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );
    }
    this.updatePreview();
  }

  removeColumn(index: number) {
    const removedItem = this.selectedColumns.splice(index, 1)[0];
    this.availableFields.push(removedItem);
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
    if (this.exportFormat === 'json') {
      this.previewData = this.getFormattedJson(true);
    } else {
      this.generateCsvPreview();
    }
  }

  private getFormattedJson(isPreview: boolean): string {
    const itemsToProcess = isPreview ? this.selectedExperiments.slice(0, 2) : this.selectedExperiments;

    const filteredItems = itemsToProcess.map(exp => {
      const filteredExp: any = { uuid: exp.uuid };

      if (this.exportFields.core.doi) filteredExp.doi = exp.doi;
      if (this.exportFields.core.createdBy) filteredExp.createdBy = exp.createdBy;
      if (this.exportFields.core.attachments && exp.attachments) filteredExp.attachments = exp.attachments;

      filteredExp.reactants = {};
      ['reactantA', 'reactantB'].forEach(r => {
        const reactantData = exp[r as keyof GetExperimentDto] as any;
        if (reactantData) {
          filteredExp.reactants[r] = {};
          if (this.exportFields.reactants.names) filteredExp.reactants[r].shortName = reactantData.shortName;
          if (this.exportFields.reactants.smiles) filteredExp.reactants[r].smiles = reactantData.smiles;
          if (this.exportFields.reactants.structures && reactantData.structures) {
            filteredExp.reactants[r].structures = reactantData.structures;
          }
        }
      });

      if (exp.measurementData) {
        filteredExp.kinetics = {};
        if (this.exportFields.kinetics.rateConstant) filteredExp.kinetics.rateConstant = exp.measurementData.rateConstant;
        if (this.exportFields.kinetics.pFO_rateConstant) filteredExp.kinetics.pFO_rateConstant = exp.measurementData.pFO_rateConstant;
        if (this.exportFields.kinetics.error) {
          filteredExp.kinetics.error = exp.measurementData.error;
          filteredExp.kinetics.isErrorPercentage = exp.measurementData.isErrorPercentage;
        }
      }

      if (exp.metadata) {
        filteredExp.metadata = {};
        if (this.exportFields.metadata.temperature) filteredExp.metadata.temperature = exp.metadata.temperature;
        if (this.exportFields.metadata.solvent) filteredExp.metadata.solvent = exp.metadata.solvent;
      }

      return filteredExp;
    });

    return JSON.stringify(filteredItems, null, 2);
  }

  private getFullCsvString(separator: string = ','): string {
    if (this.selectedColumns.length === 0) return '';
    const allRows = this.buildCsvRows(this.selectedExperiments);

    // Nutzt den TranslocoService, um die Header-Keys in echte Strings zu übersetzen
    const headers = this.selectedColumns
      .map(col => this.translocoService.translate(col.labelKey))
      .join(separator);

    const csvBody = allRows.map(row => row.map(cell => `"${cell}"`).join(separator)).join('\n');
    return headers + '\n' + csvBody;
  }

  private generateCsvPreview() {
    if (this.selectedColumns.length === 0) {
      this.tableData = [];
      return;
    }
    const allRows = this.buildCsvRows(this.selectedExperiments);
    this.tableData = allRows.slice(0, 15);
  }

  private buildCsvRows(experiments: GetExperimentDto[]): any[][] {
    return experiments.map(exp => {
      return this.selectedColumns.map(col => this.getCellValue(exp, col.id));
    });
  }

  private getCellValue(exp: GetExperimentDto, colId: string): any {
    const mapStructures = (structures: any[]) => structures ? structures.map(s => s.structure).join(' | ') : '';

    switch (colId) {
      case 'uuid': return exp.uuid || '';
      case 'doi': return exp.doi || '';
      case 'createdBy': return exp.createdBy?.email || '';

      case 'reactantA_name': return exp.reactantA?.shortName || '';
      case 'reactantA_smiles': return exp.reactantA?.smiles || '';
      case 'reactantA_struct': return mapStructures((exp.reactantA as any)?.structures);

      case 'reactantB_name': return exp.reactantB?.shortName || '';
      case 'reactantB_smiles': return exp.reactantB?.smiles || '';
      case 'reactantB_struct': return mapStructures((exp.reactantB as any)?.structures);

      case 'rateConstant': return exp.measurementData?.rateConstant ?? '';
      case 'pfo': return exp.measurementData?.pFO_rateConstant ?? '';
      case 'error': return exp.measurementData?.error != null ? `${exp.measurementData.error}${exp.measurementData.isErrorPercentage ? '%' : ''}` : '';

      case 'temperature': return (exp.metadata as any)?.temperature || '';
      case 'solvent': return (exp.metadata as any)?.solvent || '';

      default: return '';
    }
  }

  async copyToClipboard() {
    this.isCopying = true;
    this.cd.detectChanges();
    try {
      const textToCopy = this.exportFormat === 'json' ? this.getFormattedJson(false) : this.getFullCsvString('\t');
      if (textToCopy) {
        await navigator.clipboard.writeText(textToCopy);
        this.isCopied = true;
      }
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
    const timestamp = new Date().toISOString().slice(0,10);
    const content = this.exportFormat === 'json' ? this.getFormattedJson(false) : this.getFullCsvString(',');
    const filename = `experiments_${timestamp}.${this.exportFormat}`;
    const mimeType = this.exportFormat === 'json' ? 'application/json' : 'text/csv;charset=utf-8;';

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
