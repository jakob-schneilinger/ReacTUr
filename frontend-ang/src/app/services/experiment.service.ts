import { Injectable } from "@angular/core";
import { map, Observable, of, tap } from "rxjs";
import {User} from "../dtos/user";
import { AuthService } from "./auth.service";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Globals } from "../global/globals";

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
