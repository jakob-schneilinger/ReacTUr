import { Injectable } from "@angular/core";
import { delay, Observable, of } from 'rxjs';
import { AuthService } from "./auth.service";
import { HttpClient } from "@angular/common/http";
import { Globals } from "../global/globals";
import { Experiment, ExperimentData } from '../dtos/experiment';
import { MOCK_EXPERIMENT_DATA, MOCK_EXPERIMENTS } from '../mock/mock-experiments';
import { AdvancedSearch } from '../dtos/metadata';

@Injectable({
  providedIn: 'root',
})
export class ExperimentService {
  private userBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
    private authService: AuthService,
  ) {
    this.userBaseUri = this.globals.backendUri + '/experiment';
  }

  getExperimentData(experimentId: number): Observable<ExperimentData> {
    // TODO: Backend-Call:
    // return this.httpClient.get<ExperimentData>(`${this.userBaseUri}/${experimentId}/data`);

    const mockData = MOCK_EXPERIMENT_DATA.find(d => d.experimentId === experimentId);
    return of(mockData || { experimentId, measurements: [] });
  }

  getBulkExperimentData(experimentIds: number[]): Observable<ExperimentData[]> {
    const mockData = MOCK_EXPERIMENT_DATA.filter(d => experimentIds.includes(d.experimentId));
    return of(mockData).pipe(delay(800));
  }

  searchExperiment(
    reactant1: string[],
    reactant2: string[],
    metadata: AdvancedSearch
  ): Observable<Experiment[]> {

    // TODO: Späterer Backend-Call sieht ca. so aus:
    // return this.httpClient.post<Experiment[]>(`${this.userBaseUri}/search`, { reactant1, reactant2, metadata });

    // --- MOCK LOGIK START ---

    // Falls du den 'x' Check für Mock-Zwecke (leere Ergebnisse simulieren) behalten willst:
    if (reactant1.includes('x') || reactant1.includes('X')) return of([]);
    if (reactant2.includes('x') || reactant2.includes('X')) return of([]);

    const results = MOCK_EXPERIMENTS.filter((experiment) => {
      const r1Name = experiment.reactants[0]?.shortName || '';
      const r2Name = experiment.reactants[1]?.shortName || '';

      // 1. Basic Reactant Check
      // Da reactant1 & 2 jetzt IMMER Arrays sind, reicht ein einfaches .includes()
      if (reactant1 && reactant1.length > 0) {
        if (!reactant1.includes(r1Name)) return false;
      }

      if (reactant2 && reactant2.length > 0) {
        if (!reactant2.includes(r2Name)) return false;
      }

      // 2. Advanced Metadata Check
      if (metadata && Object.keys(metadata).length > 0) {
        const md = metadata;
        const meta = experiment.metadata;

        const expYear = meta.date.getFullYear().toString();
        const expMonth = (meta.date.getMonth() + 1).toString().padStart(2, '0');
        const expDay = meta.date.getDate().toString().padStart(2, '0');

        if (md.dateDay && expDay !== md.dateDay.padStart(2, '0')) return false;
        if (md.dateMonth && expMonth !== md.dateMonth.padStart(2, '0')) return false;
        if (md.dateYear && expYear !== md.dateYear) return false;

        if (md.concentrationA && meta.concentrations[0]?.toString() !== md.concentrationA) return false;
        if (md.concentrationB && meta.concentrations[1]?.toString() !== md.concentrationB) return false;
        if (md.wavelength && meta.wavelength?.toString() !== md.wavelength) return false;

        if (md.scientist && !meta.scientist.toLowerCase().includes(md.scientist.toLowerCase())) return false;
        if (md.solvent && !meta.solvent.toLowerCase().includes(md.solvent.toLowerCase())) return false;

        if (md.instrument && (!meta.instrument || !meta.instrument.toLowerCase().includes(md.instrument.toLowerCase()))) return false;
        if (md.modality && (!meta.modality || !meta.modality.toLowerCase().includes(md.modality.toLowerCase()))) return false;
        if (md.comment && (!meta.comment || !meta.comment.toLowerCase().includes(md.comment.toLowerCase()))) return false;

        if (md.tempFrom && meta.temperature < parseFloat(md.tempFrom)) return false;
        if (md.tempTo && meta.temperature > parseFloat(md.tempTo)) return false;
      }

      return true;
    });

    // Simuliere Netzwerk-Verzögerung von 1ms
    return of(results);
  }

  searchAdvancedExperiment() {
    //TODO: search Experiment with advanced search parameters
  }

  createExperiment() {
    //TODO: either without params -> return id
    //TODO: or with params (e.g. reactant) -> return id and maybe data?
  }

  getExperiment() {
    //TODO: get Experiment with id
  }

  updateExperiment() {
    //TODO: update Experiment with id and new data
  }

  deleteExperiment() {
    //TODO: update Experiment with id and new data
  }

  addFile() {
    //TODO: extra function for file upload? easier for backend?
  }

  getFiles(id: number):Observable<File[]> {
    return of([]);
  }

  removeFile() {
    //TODO: remove file from experiment
  }

}
