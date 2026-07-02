//TODO: overthink interface
import { ChemicalTypeDto } from '../../shared/dtos/selection-option.dto';
import { DetailsDto } from '../../shared/dtos/details.dto';
import { StructureFlag } from './structure-flag.enum';

export interface ReactantDto extends DetailsDto {
  smiles: string;
  chemicalType: ChemicalTypeDto;
  shortName: string;
  chemicalName?: string;
  flag?: StructureFlag;
}

export interface ReactantDetailDto extends ReactantDto {
  additionalShortNames?: string[];
  casNumber?: string;
  inChiKey?: string;
  comment?: string;
  references?: string[];
  structures?: StructureDto[];
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

export interface StructureDto {
  id?: string;
  reactant?: string; //safeguard
  name?: string;
  structure?: string;
  flag?: StructureFlag;
}
