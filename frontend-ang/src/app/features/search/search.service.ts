import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { Globals } from '../../core/global/globals';

import { SearchParametersDto } from '../search/search.dto';
import { GetExperimentDto } from '../experiments/experiment.dto';
import { Page } from './page.dto';

@Injectable({
  providedIn: 'root',
})
export class SearchService {
  private experimentBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
  ) {
    this.experimentBaseUri = this.globals.backendUri + '/experiment';
  }

  searchExperiments(
    searchParams: SearchParametersDto,
    page: number = 0,
    size: number = 25,
  ): Observable<Page<GetExperimentDto>> {
    let params = this.buildHttpParams(searchParams);

    params = params.append('page', page.toString());
    params = params.append('size', size.toString());

    return this.httpClient.get<Page<GetExperimentDto>>(this.experimentBaseUri, { params });
  }

  private buildHttpParams(params: SearchParametersDto): HttpParams {
    let httpParams = new HttpParams();
    const appendParams = (obj: any, prefix: string = '') => {
      if (!obj) return;
      Object.keys(obj).forEach((key) => {
        const value = obj[key];

        if (value !== null && value !== undefined && value !== '') {
          const paramKey = prefix ? `${prefix}.${key}` : key;

          if (typeof value === 'object' && !Array.isArray(value) && !(value instanceof Date)) {
            appendParams(value, paramKey);
          } else {
            httpParams = httpParams.set(paramKey, value.toString());
          }
        }
      });
    };

    appendParams(params);
    return httpParams;
  }
}
