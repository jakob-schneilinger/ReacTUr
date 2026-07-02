import { Injectable } from '@angular/core';
import { map, Observable, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { HttpClient } from '@angular/common/http';
import { Globals } from '../../core/global/globals';
import { MOCK_REACTANT_DETAILS } from '../../mock/mock-reactants';
import {
  ReactantDetailDto,
  ReactantDto,
  SaveReactantDto,
  StructureDto,
} from './reactant.dto';
import { StructureFlag } from '../../shared/dtos/enums/structure-flag.enum';
import { mockStructure1, mockStructure2 } from '../../mock/mock-structures';

@Injectable({
  providedIn: 'root',
})
export class ReactantService {
  private reactantBaseUri: string;
  private fileBaseUri: string;

  private reactantsDb: ReactantDetailDto[] = [...(MOCK_REACTANT_DETAILS as ReactantDetailDto[])];

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
    private authService: AuthService,
  ) {
    this.reactantBaseUri = this.globals.backendUri + '/reactant';
    this.fileBaseUri = this.globals.backendUri + '/file';
  }

  getAllReactants(): Observable<ReactantDto[]> {
    return this.httpClient.get<ReactantDto[]>(this.reactantBaseUri);
  }

  getArchivedReactants(): Observable<ReactantDto[]> {
    return this.httpClient.get<ReactantDto[]>(`${this.reactantBaseUri}/archived`);
  }

  getReactantDetail(uuid: string): Observable<ReactantDetailDto> {
    return this.httpClient.get<ReactantDetailDto>(`${this.reactantBaseUri}/${uuid}`);
  }

  getReactantDetailMock(uuid: string): Observable<ReactantDetailDto> {
    return this.httpClient.get<ReactantDetailDto>(`${this.reactantBaseUri}/${uuid}`).pipe(
      map((reactant: ReactantDetailDto) => {
        const random = Math.random();
        let structures: StructureDto[] = [];

        if (random < 0.3) {
          structures.push({
            id: 'structure-1',
            reactant: uuid,
            name: 'mockStructure1.xyz',
            structure: mockStructure1,
            flag: StructureFlag.READY,
          });

          if (random < 0.1) {
            structures.push({
              id: 'structure-2',
              reactant: uuid,
              name: 'mockStructure2.xyz',
              structure: mockStructure2,
              flag: StructureFlag.READY,
            });
          }
        } else if (random < 0.5) {
          structures.push({
            flag: StructureFlag.FAILED,
          });
        } else {
          structures.push({
            flag: StructureFlag.PENDING,
          });
        }

        console.log('mocked structures:', structures);
        const final: ReactantDetailDto = {
          ...reactant,
          structures: structures,
        };

        console.log('final reactant:', final);

        return final;
      }),
    );

    return this.httpClient.get<ReactantDetailDto>(`${this.reactantBaseUri}/${uuid}`);
  }

  createReactant(saveReactant: SaveReactantDto): Observable<ReactantDetailDto> {
    return this.httpClient.post<ReactantDetailDto>(this.reactantBaseUri, saveReactant);
  }

  updateReactant(uuid: string, saveReactant: SaveReactantDto): Observable<ReactantDetailDto> {
    return this.httpClient.patch<ReactantDetailDto>(
      `${this.reactantBaseUri}/${uuid}`,
      saveReactant,
    );
  }

  archiveReactant(uuid: string): Observable<void> {
    return this.httpClient.delete<void>(`${this.reactantBaseUri}/${uuid}`);
  }

  restoreReactant(uuid: string): Observable<ReactantDto> {
    return this.httpClient.put<ReactantDto>(`${this.reactantBaseUri}/${uuid}/restore`, {});
  }
}
