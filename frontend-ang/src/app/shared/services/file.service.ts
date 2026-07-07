import { Injectable } from '@angular/core';
import { Globals } from '../../core/global/globals';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class FileService {
  private reactantFileBaseUri: string;
  private experimentFileBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
  ) {
    this.reactantFileBaseUri = this.globals.backendUri + '/structure/file';
    this.experimentFileBaseUri = this.globals.backendUri + '/file';
  }

}
