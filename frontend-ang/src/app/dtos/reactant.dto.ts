//TODO: overthink interface
import { ChemicalTypeDto } from './selection-option.dto';
import { DetailsDto } from './details.dto';

// old
export interface ReactantDtoOld {
  shortName: string;
  smiles: string;
  type1: string;
  chemicalName?: string;
  additionalShortNames?: string[];
  casNumber?: string;
  comment?: string;
  references?: string[];
  structures?: File[];
}

// new
export interface ReactantDto extends DetailsDto{
  smiles: string;
  chemicalType: ChemicalTypeDto;
  shortName: string;
  chemicalName?: string;
}

export interface ReactantDetailDto extends ReactantDto {
  additionalShortNames?: string[];
  casNumber?: string;
  inChiKey?: string;
  comment?: string;
  references?: string[];
  structures?: File[];
}

export interface SaveReactantDto {
  smiles: string;
  chemicalType: string; //uuid TODO: think about renaming (e.g. to chemicalTypeUuid) also with all other uuid strings
  shortName: string;
  additionalShortNames?: string[];
  chemicalName?: string;
  casNumber?: string;
  inChiKey?: string;
  comment?: string;
  references?: string[];
}
