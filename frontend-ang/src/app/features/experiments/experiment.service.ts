import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { Globals } from '../../core/global/globals';

import { ExperimentDataDto, GetExperimentDto, SaveExperimentDto } from './experiment.dto';
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
    return this.httpClient.post<GetExperimentDto>(this.experimentBaseUri, experimentData);
  }

  updateExperiment(uuid: string, experimentData: SaveExperimentDto): Observable<GetExperimentDto> {
    return this.httpClient.put<GetExperimentDto>(
      `${this.experimentBaseUri}/${uuid}`,
      experimentData,
    );
  }


  // if archived just for creator, scientist or admin visible
  getExperiment(uuid: string): Observable<GetExperimentDto> {
    return this.httpClient.get<GetExperimentDto>(`${this.experimentBaseUri}/${uuid}`);
  }

  getAuditLogs(uuid: string): Observable<AuditLogDto[]> {
    return this.httpClient.get<AuditLogDto[]>(`${this.experimentBaseUri}/${uuid}/audit_logs`);
  }

  // normal user gets own archived experiments, admin gets all
  getArchivedExperiments(): Observable<GetExperimentDto[]> {
    return this.httpClient.get<GetExperimentDto[]>(`${this.experimentBaseUri}/archived`);
  }

  // only admin, creator or scientist can restore
  archiveExperiment(uuid: string): Observable<void> {
    return this.httpClient.delete<void>(`${this.experimentBaseUri}/${uuid}`);
  }

  // only admin, creator or scientist can restore
  restoreExperiment(uuid: string): Observable<GetExperimentDto> {
    return this.httpClient.put<GetExperimentDto>(`${this.experimentBaseUri}/${uuid}/restore`, {});
  }

  getExperimentHistory(uuid: string): Observable<ExperimentHistoryDto[]> {
    return this.httpClient.get<ExperimentHistoryDto[]>(`${this.experimentBaseUri}/${uuid}/history`);
  }

}
