import { Injectable } from '@angular/core';
import { delay, Observable, of } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AuthService } from './auth.service';
import { Globals } from '../global/globals';

// Updated DTO Imports
import { ExperimentDataDto, GetExperimentDto, SaveExperimentDto } from '../dtos/experiment.dto';
import { SearchParametersDto } from '../dtos/search.dto';
import { MOCK_EXPERIMENT_DATA, MOCK_EXPERIMENTS } from '../mock/mock-experiments';
import { AuditLogDto } from '../dtos/audit.dto';

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
    return of(MOCK_EXPERIMENTS[0]).pipe(delay(500));
    return this.httpClient.post<GetExperimentDto>(this.experimentBaseUri, experimentData);
  }

  patchExperiment(uuid: string, experimentData: SaveExperimentDto): Observable<GetExperimentDto> {
    //TODO: remove when backend connected
    const mockExp = MOCK_EXPERIMENTS.find((e) => e.uuid === uuid) || MOCK_EXPERIMENTS[0];
    return of(mockExp).pipe(delay(500));
    return this.httpClient.patch<GetExperimentDto>(
      `${this.experimentBaseUri}/${uuid}`,
      experimentData,
    );
  }

  getExperiment(uuid: string): Observable<GetExperimentDto> {
    //TODO: remove when backend connected
    const mockExp = MOCK_EXPERIMENTS.find((e) => e.uuid === uuid) || MOCK_EXPERIMENTS[0];
    return of(mockExp).pipe(delay(500));
    return this.httpClient.get<GetExperimentDto>(`${this.experimentBaseUri}/${uuid}`);
  }

  searchExperiments(searchParams: SearchParametersDto): Observable<GetExperimentDto[]> {
    //TODO: remove when backend connected
    return this.mockSearchExperiment(searchParams);
    const params = this.buildHttpParams(searchParams);
    return this.httpClient.get<GetExperimentDto[]>(this.experimentBaseUri, { params });
  }

  getAuditLogs(uuid: string): Observable<AuditLogDto[]> {
    //TODO: remove when backend connected
    return of([] as AuditLogDto[]).pipe(delay(500)); // Mocking empty logs
    return this.httpClient.get<AuditLogDto[]>(`${this.experimentBaseUri}/${uuid}/audit_logs`);
  }

  getArchivedExperiments(): Observable<GetExperimentDto[]> {
    //TODO: remove when backend connected
    return of(MOCK_EXPERIMENTS.slice(0, 2)).pipe(delay(500)); // Mocking a couple archived
    return this.httpClient.get<GetExperimentDto[]>(`${this.experimentBaseUri}/archived`);
  }

  archiveExperiment(uuid: string): Observable<void> {
    //TODO: remove when backend connected
    return of(undefined).pipe(delay(500));
    return this.httpClient.delete<void>(`${this.experimentBaseUri}/${uuid}`);
  }

  restoreExperiment(uuid: string): Observable<GetExperimentDto> {
    //TODO: remove when backend connected
    const mockExp = MOCK_EXPERIMENTS.find((e) => e.uuid === uuid) || MOCK_EXPERIMENTS[0];
    return of(mockExp).pipe(delay(500));
    return this.httpClient.put<GetExperimentDto>(`${this.experimentBaseUri}/${uuid}/restore`, {});
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

  // --- FILE ENDPOINTS ---

  uploadFile(experimentUuid: string, file: File): Observable<any> {
    //TODO: remove when backend connected
    return of({ status: 'Mock Upload Success' }).pipe(delay(1000));
    const formData = new FormData();
    formData.append('file', file);
    return this.httpClient.post(`${this.fileBaseUri}/${experimentUuid}/`, formData);
  }

  removeFile(experimentUuid: string, fileUuid: string): Observable<void> {
    //TODO: remove when backend connected
    return of(undefined).pipe(delay(500));
    return this.httpClient.delete<void>(`${this.fileBaseUri}/${experimentUuid}/${fileUuid}`);
  }

  // --- UTILITIES & MOCK IMPLEMENTATIONS ---

  private mockSearchExperiment(searchParams: SearchParametersDto): Observable<GetExperimentDto[]> {
    const results = MOCK_EXPERIMENTS.filter((experiment) => {
      const meta = experiment.metadata;

      if (searchParams) {
        if (searchParams.reactantA?.length > 0) {
          if (!searchParams.reactantA.includes(experiment.reactantA.uuid)) return false;
        }
        if (searchParams.reactantB?.length > 0) {
          if (!searchParams.reactantB.includes(experiment.reactantB.uuid)) return false;
        }

        const expDate = new Date(meta.date).getTime();
        if (searchParams.dateMin && expDate < new Date(searchParams.dateMin).getTime())
          return false;
        if (searchParams.dateMax && expDate > new Date(searchParams.dateMax).getTime())
          return false;

        if (searchParams?.scientist?.length > 0) {
          if (!searchParams.scientist.includes(meta.scientist.uuid)) return false;
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

  private buildHttpParams(params: SearchParametersDto): HttpParams {
    let httpParams = new HttpParams();
    if (params) {
      Object.keys(params).forEach((key) => {
        const value = (params as any)[key];
        if (value !== null && value !== undefined && value !== '') {
          httpParams = httpParams.set(key, value.toString());
        }
      });
    }
    return httpParams;
  }
}
