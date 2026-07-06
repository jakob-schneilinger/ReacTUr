import { Injectable } from '@angular/core';
import { delay, Observable, of } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { Globals } from '../../core/global/globals';

import { ExperimentDataDto, GetExperimentDto, SaveExperimentDto } from './experiment.dto';
import { SearchParametersDto } from '../search/search.dto';
import { MOCK_EXPERIMENT_DATA, MOCK_EXPERIMENTS } from '../../mock/mock-experiments';
import { AuditLogDto } from '../../shared/dtos/audit.dto';
import { ExperimentHistoryDto } from './experiment-history.dto';

@Injectable({
  providedIn: 'root',
})
export class ExperimentService {
  private experimentBaseUri: string;
  private fileBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
    private authService: AuthService,
  ) {
    this.experimentBaseUri = this.globals.backendUri + '/experiment';
    this.fileBaseUri = this.globals.backendUri + '/file';
  }

  // --- EXPERIMENT ENDPOINTS ---

  createExperiment(experimentData: SaveExperimentDto): Observable<GetExperimentDto> {
    //TODO: remove when backend connected
    //return of(MOCK_EXPERIMENTS[0]).pipe(delay(500));
    return this.httpClient.post<GetExperimentDto>(this.experimentBaseUri, experimentData);
  }

  updateExperiment(uuid: string, experimentData: SaveExperimentDto): Observable<GetExperimentDto> {
    //TODO: remove when backend connected
    const mockExp = MOCK_EXPERIMENTS.find((e) => e.uuid === uuid) || MOCK_EXPERIMENTS[0];
    // return of(mockExp).pipe(delay(500));
    return this.httpClient.put<GetExperimentDto>(
      `${this.experimentBaseUri}/${uuid}`,
      experimentData,
    );
  }


  // if archived just for creator, scientist or admin visible
  getExperiment(uuid: string): Observable<GetExperimentDto> {
    //TODO: remove when backend connected
    const mockExp = MOCK_EXPERIMENTS.find((e) => e.uuid === uuid) || MOCK_EXPERIMENTS[0];
    // return of(mockExp).pipe(delay(500));
    return this.httpClient.get<GetExperimentDto>(`${this.experimentBaseUri}/${uuid}`);
  }

  getAuditLogs(uuid: string): Observable<AuditLogDto[]> {
    //TODO: remove when backend connected
    //return of([] as AuditLogDto[]).pipe(delay(500)); // Mocking empty logs
    return this.httpClient.get<AuditLogDto[]>(`${this.experimentBaseUri}/${uuid}/audit_logs`);
  }

  // normal user gets own archived experiments, admin gets all
  getArchivedExperiments(): Observable<GetExperimentDto[]> {
    //TODO: remove when backend connected
    //return of(MOCK_EXPERIMENTS.slice(0, 2)).pipe(delay(500)); // Mocking a couple archived
    return this.httpClient.get<GetExperimentDto[]>(`${this.experimentBaseUri}/archived`);
  }

  // only admin, creator or scientist can restore
  archiveExperiment(uuid: string): Observable<void> {
    //TODO: remove when backend connected
    //return of(undefined).pipe(delay(500));
    return this.httpClient.delete<void>(`${this.experimentBaseUri}/${uuid}`);
  }

  // only admin, creator or scientist can restore
  restoreExperiment(uuid: string): Observable<GetExperimentDto> {
    //TODO: remove when backend connected
    const mockExp = MOCK_EXPERIMENTS.find((e) => e.uuid === uuid) || MOCK_EXPERIMENTS[0];
    //return of(mockExp).pipe(delay(500));
    return this.httpClient.put<GetExperimentDto>(`${this.experimentBaseUri}/${uuid}/restore`, {});
  }

  getExperimentHistory(uuid: string): Observable<ExperimentHistoryDto[]> {
    return this.httpClient.get<ExperimentHistoryDto[]>(`${this.experimentBaseUri}/${uuid}/history`);
  }

  // --- DATA ENDPOINTS ---

  getExperimentData(experimentId: string): Observable<ExperimentDataDto> {
    //TODO: remove when backend connected
    const mockData = MOCK_EXPERIMENT_DATA.find((d) => d.experimentId === experimentId);
    return of(mockData || { experimentId, measurements: [] }).pipe(delay(500));
    return this.httpClient.get<ExperimentDataDto>(`${this.experimentBaseUri}/${experimentId}/data`);
  }

  getBulkExperimentData(experimentIds: string[]): Observable<ExperimentDataDto[]> {
    //TODO: remove when backend connected
    const mockData = MOCK_EXPERIMENT_DATA.filter((d) => experimentIds.includes(d.experimentId));
    return of(mockData).pipe(delay(800));
    const params = new HttpParams().set('ids', experimentIds.join(','));
    return this.httpClient.get<ExperimentDataDto[]>(`${this.experimentBaseUri}/bulk-data`, {
      params,
    });
  }

  // --- UTILITIES & MOCK IMPLEMENTATIONS ---

  private mockSearchExperiment(searchParams: SearchParametersDto): Observable<GetExperimentDto[]> {
    console.log('searchParams', searchParams);
    const results = MOCK_EXPERIMENTS.filter((experiment) => {
      const meta = experiment.metadata;

      if (searchParams) {
        const searchA = searchParams.reactantA || [];
        const searchB = searchParams.reactantB || [];
        const expA = experiment.reactantA.uuid;
        const expB = experiment.reactantB.uuid;

        if (searchA.length > 0 && searchB.length > 0) {
          // Both fields have input (direct match OR cross match)
          const directMatch = searchA.includes(expA) && searchB.includes(expB);
          const crossMatch = searchA.includes(expB) && searchB.includes(expA);

          if (!directMatch && !crossMatch) return false;
        } else if (searchA.length > 0) {
          // Only Reactant A (check against both experiment reactants)
          if (!searchA.includes(expA) && !searchA.includes(expB)) return false;
        } else if (searchB.length > 0) {
          // Only Reactant B (check against both experiment reactants)
          if (!searchB.includes(expA) && !searchB.includes(expB)) return false;
        }

        const expDate = new Date(meta.date).getTime();
        if (searchParams.dateMin && expDate < new Date(searchParams.dateMin).getTime())
          return false;
        if (searchParams.dateMax && expDate > new Date(searchParams.dateMax).getTime())
          return false;

        if (searchParams?.scientist?.length > 0) {
          if (!searchParams.scientist.includes(meta.scientist.id)) return false;
        }
        if (searchParams.solvent && meta.solvent.uuid !== searchParams.solvent) return false;
        if (searchParams.instrument && meta.instrument?.uuid !== searchParams.instrument)
          return false;
        if (searchParams.modality && meta.modality?.uuid !== searchParams.modality) return false;

        if (searchParams.temperature_Min && meta.temperature < searchParams.temperature_Min)
          return false;
        if (searchParams.temperature_Max && meta.temperature > searchParams.temperature_Max)
          return false;

        if (
          searchParams.concentrationA_Min &&
          meta.concentrationA < searchParams.concentrationA_Min
        )
          return false;
        if (
          searchParams.concentrationA_Max &&
          meta.concentrationA > searchParams.concentrationA_Max
        )
          return false;

        if (
          searchParams.comment &&
          !meta.comment?.toLowerCase().includes(searchParams.comment.toLowerCase())
        )
          return false;
      }
      return true;
    });

    return of(results).pipe(delay(500));
  }

}
