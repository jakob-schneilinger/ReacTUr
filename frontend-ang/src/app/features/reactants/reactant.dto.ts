//TODO: overthink interface
import { ChemicalTypeDto } from '../../shared/dtos/selection-option.dto';
import { StructureFlag } from './structure-flag.enum';

/**
 * DTO representing the basic information of a reactant.
 */
export interface ReactantDto {
  uuid: string;
  smiles: string;
  chemicalType?: ChemicalTypeDto; // TODO: should be mandatory in the future
  shortName: string;
  chemicalName?: string;
  flag?: StructureFlag;
}

/**
 * DTO representing the comprehensive details of a reactant.
 */
export interface ReactantDetailDto extends ReactantDto {
  additionalShortNames?: string[];
  casNumber?: string;
  inChiKey?: string;
  comment?: string;
  references?: string[];
  archivedAt?: string;
  structures?: StructureDto[];
}

/**
 * DTO used for creating and updating a reactant in the database.
 */
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

/**
 * DTO representing a 3D structure associated with a reactant.
 */
export interface StructureDto {
  id: string;        // UUID
  reactant: string;  // UUID
  name: string;
  structure: string;
  flag: StructureFlag;
}
