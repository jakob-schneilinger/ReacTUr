import { Injectable } from '@angular/core';
import { Globals } from '../../core/global/globals';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { StructureDto } from '../../features/reactants/reactant.dto';
import { StructureFlag } from '../dtos/enums/structure-flag.enum';
import { mockStructure1 } from '../../mock/mock-structures';

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
    this.experimentFileBaseUri = this.globals.backendUri + '/file'; //TODO: Set correct path
  }

  uploadStructure() {
    // post /reactantID -> send xyz. file -> return StructureDto
  }

  generateStructureFromApi(reactantId: string, smiles: string): Observable<void> {
    return of();

    // post /reactantID/generate -> void -> return void
  }

  selectStructure(structureId: string): Observable<StructureDto> {
    return of({
      id: structureId,
      reactant: '1c433604-7b3d-4b0e-b8f2-9770e0c9edab',
      name: 'Name',
      structure: mockStructure1,
      flag: StructureFlag.ACTIVE,
    } as StructureDto);
    // post /structureID/select -> void -> return StructureDto
  }
}
