import { Injectable } from "@angular/core";
import { delay, Observable, of } from 'rxjs';
import { AuthService } from "./auth.service";
import { HttpClient } from "@angular/common/http";
import { Globals } from "../global/globals";
import { ExperimentData } from '../dtos/experiment';
import { MOCK_EXPERIMENT_DATA } from '../mock/mock-experiments';

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

  searchExperiment() {
    //TODO: search Experiment with search parameters (reactant1 and reactant2)
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

  removeFile() {
    //TODO: remove file from experiment
  }

}
