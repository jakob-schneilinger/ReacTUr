import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDropList, CdkDrag, CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';

import { GetExperimentDto } from '../../../experiments/experiment.dto';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { catchError, forkJoin, of } from 'rxjs';
import { ReactantService } from '../../../reactants/reactant.service';
import { ReactantDetailDto } from '../../../reactants/reactant.dto';

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
  imports: [CommonModule, FormsModule, CdkDropList, CdkDrag, TRANSLOCO_IMPORTS],
})
export class ExportModalComponent implements OnInit {
  @Input() selectedExperiments: GetExperimentDto[] = [];
  @Output() close = new EventEmitter<void>();

  exportFormat: 'json' | 'csv' = 'json';
  isCopied: boolean = false;
  isCopying: boolean = false;

  isLoading: boolean = true;
  reactantDetailsMap: Map<string, ReactantDetailDto> = new Map();

  exportFields = {
    core: { doi: true, attachments: false },
    reactants: { names: true, smiles: true, structures: true, type: true },
    kinetics: { rateConstant: true, pFO_rateConstant: false, error: true, isErrorPercentage: false },
    metadata: {
      date: true,
      scientist: true,
      temperature: true,
      solvent: true,
      concentrationA: true,
      concentrationB: true,
      instrument: false,
      modality: false,
      wavelength: false,
      comment: false
    }
  };

  private readonly ALL_FIELDS: ExportField[] = [
    { id: 'uuid', labelKey: 'exportModal.fields.uuid', category: 'core' },
    { id: 'doi', labelKey: 'exportModal.fields.doi', category: 'core' },

    { id: 'reactantA_name', labelKey: 'exportModal.fields.reactantAName', category: 'reactants' },
    { id: 'reactantA_smiles', labelKey: 'exportModal.fields.reactantASmiles', category: 'reactants' },
    { id: 'reactantA_struct', labelKey: 'exportModal.fields.reactantAStruct', category: 'reactants' },
    { id: 'reactantA_type', labelKey: 'exportModal.fields.reactantAType', category: 'reactants' },

    { id: 'reactantB_name', labelKey: 'exportModal.fields.reactantBName', category: 'reactants' },
    { id: 'reactantB_smiles', labelKey: 'exportModal.fields.reactantBSmiles', category: 'reactants' },
    { id: 'reactantB_struct', labelKey: 'exportModal.fields.reactantBStruct', category: 'reactants' },
    { id: 'reactantB_type', labelKey: 'exportModal.fields.reactantBType', category: 'reactants' },

    { id: 'rateConstant', labelKey: 'exportModal.fields.rateConstant', category: 'kinetics' },
    { id: 'pfo', labelKey: 'exportModal.fields.pfo', category: 'kinetics' },
    { id: 'error', labelKey: 'exportModal.fields.error', category: 'kinetics' },
    { id: 'isErrorPercentage', labelKey: 'exportModal.fields.isErrorPercentage', category: 'kinetics' },

    { id: 'date', labelKey: 'exportModal.fields.date', category: 'metadata' },
    { id: 'scientist', labelKey: 'exportModal.fields.scientist', category: 'metadata' },
    { id: 'temperature', labelKey: 'exportModal.fields.temperature', category: 'metadata' },
    { id: 'solvent', labelKey: 'exportModal.fields.solvent', category: 'metadata' },
    { id: 'concentrationA', labelKey: 'exportModal.fields.concentrationA', category: 'metadata' },
    { id: 'concentrationB', labelKey: 'exportModal.fields.concentrationB', category: 'metadata' },
    { id: 'instrument', labelKey: 'exportModal.fields.instrument', category: 'metadata' },
    { id: 'modality', labelKey: 'exportModal.fields.modality', category: 'metadata' },
    { id: 'wavelength', labelKey: 'exportModal.fields.wavelength', category: 'metadata' },
    { id: 'expComment', labelKey: 'exportModal.fields.expComment', category: 'metadata' }
  ];

  private readonly DEFAULT_COLUMN_IDS = [
    'reactantA_name', 'reactantB_name',
    'reactantA_smiles', 'reactantB_smiles',
    'reactantA_struct', 'reactantB_struct',
    'reactantA_type', 'reactantB_type',
    'rateConstant'
  ];

  availableFields: ExportField[] = [];
  selectedColumns: ExportField[] = [];

  previewData: string = '';
  tableData: any[][] = [];

  constructor(
    private cd: ChangeDetectorRef,
    private translocoService: TranslocoService,
    private reactantService: ReactantService
  ) {}

  ngOnInit() {
    this.initializeDragAndDropLists();
    this.loadReactantDetails();
  }

  private loadReactantDetails() {
    this.isLoading = true;
    const uniqueReactantUuids = new Set<string>();

    this.selectedExperiments.forEach(exp => {
      if (exp.reactantA?.uuid) uniqueReactantUuids.add(exp.reactantA.uuid);
      if (exp.reactantB?.uuid) uniqueReactantUuids.add(exp.reactantB.uuid);
    });

    if (uniqueReactantUuids.size === 0) {
      this.isLoading = false;
      this.updatePreview();
      return;
    }

    const requests = Array.from(uniqueReactantUuids).map(uuid =>
      this.reactantService.getReactantDetail(uuid).pipe(
        catchError(err => {
          console.error(`Failed to load details for reactant ${uuid}`, err);
          return of(null);
        })
      )
    );

    forkJoin(requests).subscribe(details => {
      details.forEach(detail => {
        if (detail && detail.uuid) {
          this.reactantDetailsMap.set(detail.uuid, detail);
        }
      });
      this.isLoading = false;
      this.updatePreview();
      this.cd.detectChanges();
    });
  }

  private initializeDragAndDropLists() {
    this.ALL_FIELDS.forEach(field => {
      if (this.DEFAULT_COLUMN_IDS.includes(field.id)) {
        this.selectedColumns.push(field);
      } else {
        this.availableFields.push(field);
      }
    });

    this.selectedColumns.sort((a, b) => {
      return this.DEFAULT_COLUMN_IDS.indexOf(a.id) - this.DEFAULT_COLUMN_IDS.indexOf(b.id);
    });
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
    if (this.isLoading) return; // Warten, bis Daten da sind

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

      if (this.exportFields.core.doi && exp.doi) filteredExp.doi = exp.doi;
      if (this.exportFields.core.attachments && exp.attachments) filteredExp.attachments = exp.attachments;

      const reactantsObj: any = {};
      ['reactantA', 'reactantB'].forEach(r => {
        const reactantData = exp[r as keyof GetExperimentDto] as any;
        if (reactantData) {
          const rObj: any = {};
          if (this.exportFields.reactants.names) rObj.shortName = reactantData.shortName;
          if (this.exportFields.reactants.smiles) rObj.smiles = reactantData.smiles;
          if (this.exportFields.reactants.type) rObj.type = this.extractNestedLabel(reactantData.chemicalType);

          // NEU: Greife auf die Map zu, um die 3D Struktur ins JSON zu packen
          if (this.exportFields.reactants.structures) {
            const detail = this.reactantDetailsMap.get(reactantData.uuid);
            if (detail?.structures && detail.structures.length > 0) {
              rObj.structures = detail.structures;
            }
          }

          if (Object.keys(rObj).length > 0) {
            reactantsObj[r] = rObj;
          }
        }
      });
      if (Object.keys(reactantsObj).length > 0) {
        filteredExp.reactants = reactantsObj;
      }

      if (exp.measurementData) {
        const kineticsObj: any = {};
        if (this.exportFields.kinetics.rateConstant) kineticsObj.rateConstant = exp.measurementData.rateConstant;
        if (this.exportFields.kinetics.pFO_rateConstant) kineticsObj.pFO_rateConstant = exp.measurementData.pFO_rateConstant;
        if (this.exportFields.kinetics.error) kineticsObj.error = exp.measurementData.error;
        if (this.exportFields.kinetics.isErrorPercentage) kineticsObj.isErrorPercentage = exp.measurementData.isErrorPercentage;

        if (Object.keys(kineticsObj).length > 0) {
          filteredExp.kinetics = kineticsObj;
        }
      }

      if (exp.metadata) {
        const metaObj: any = {};
        if (this.exportFields.metadata.date) metaObj.date = exp.metadata.date;
        if (this.exportFields.metadata.scientist) metaObj.scientist = exp.metadata.scientist;
        if (this.exportFields.metadata.temperature) metaObj.temperature = exp.metadata.temperature;
        if (this.exportFields.metadata.solvent) metaObj.solvent = exp.metadata.solvent;
        if (this.exportFields.metadata.concentrationA) metaObj.concentrationA = exp.metadata.concentrationA;
        if (this.exportFields.metadata.concentrationB) metaObj.concentrationB = exp.metadata.concentrationB;
        if (this.exportFields.metadata.instrument) metaObj.instrument = exp.metadata.instrument;
        if (this.exportFields.metadata.modality) metaObj.modality = exp.metadata.modality;
        if (this.exportFields.metadata.wavelength) metaObj.wavelength = exp.metadata.wavelength;
        if (this.exportFields.metadata.comment) metaObj.comment = exp.metadata.comment;

        if (Object.keys(metaObj).length > 0) {
          filteredExp.metadata = metaObj;
        }
      }

      return filteredExp;
    });

    return JSON.stringify(filteredItems, null, 2);
  }

  private getFullCsvString(separator: string = ','): string {
    if (this.selectedColumns.length === 0) return '';
    const allRows = this.buildCsvRows(this.selectedExperiments);

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
    const getStructures = (reactant: any) => {
      if (!reactant || !reactant.uuid) return '';
      const detail = this.reactantDetailsMap.get(reactant.uuid);
      return detail?.structures ? detail.structures.map(s => s.structure).join(' | ') : '';
    };

    const m = exp.metadata || {};

    switch (colId) {
      case 'uuid': return exp.uuid || '';
      case 'doi': return exp.doi || '';

      case 'reactantA_name': return exp.reactantA?.shortName || '';
      case 'reactantA_smiles': return exp.reactantA?.smiles || '';
      case 'reactantA_struct': return getStructures(exp.reactantA);
      case 'reactantA_type': return this.extractNestedLabel(exp.reactantA?.chemicalType);

      case 'reactantB_name': return exp.reactantB?.shortName || '';
      case 'reactantB_smiles': return exp.reactantB?.smiles || '';
      case 'reactantB_struct': return getStructures(exp.reactantB);
      case 'reactantB_type': return this.extractNestedLabel(exp.reactantB?.chemicalType);

      case 'rateConstant': return exp.measurementData?.rateConstant ?? '';
      case 'pfo': return exp.measurementData?.pFO_rateConstant ?? '';
      case 'error': return exp.measurementData?.error ?? '';
      case 'isErrorPercentage': return exp.measurementData?.isErrorPercentage ? 'True' : 'False';

      case 'date': return m.date ? new Date(m.date).toLocaleDateString() : '';
      case 'scientist': return m.scientist ? `${m.scientist.firstName} ${m.scientist.lastName}` : '';
      case 'temperature': return m.temperature ?? '';
      case 'solvent': return this.extractNestedLabel(m.solvent);
      case 'concentrationA': return m.concentrationA ?? '';
      case 'concentrationB': return m.concentrationB ?? '';
      case 'instrument': return this.extractNestedLabel(m.instrument);
      case 'modality': return this.extractNestedLabel(m.modality);
      case 'wavelength': return m.wavelength ?? '';
      case 'expComment': return m.comment || '';

      default: return '';
    }
  }

  private extractNestedLabel(obj: any): string {
    if (!obj) return '';
    if (typeof obj === 'string') return obj;
    return obj.name || obj.label || obj.value || JSON.stringify(obj);
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
