import { InstrumentDto, ModalityDto, SolventDto } from './selection-option.dto';
import { DetailsDto } from './details.dto';
import { GetUserDto } from './user.dto';

// old
export interface MetadataDto {
  date: Date;
  scientist: string;
  temperature: number;
  solvent: string;
  concentrations: number[];
  instrument?: string;
  modality?: string;
  wavelength?: number;
  rawDataFile?: File;
  comment?: string;
}

export interface AdvancedSearchDto {
  dateDay?: string;
  dateMonth?: string;
  dateYear?: string;
  scientist?: string;
  tempFrom?: string;
  tempTo?: string;
  solvent?: string;
  concentrationA?: string;
  concentrationB?: string;
  instrument?: string;
  modality?: string;
  wavelength?: string;
  comment?: string;
  minRange?: string;
  maxRange?: string;
}

// new
export interface GetMetadataDto extends DetailsDto {
  date: string;
  scientist: GetUserDto;
  temperature: number;
  solvent: SolventDto;
  concentrationA: number;
  concentrationB: number;
  instrument?: InstrumentDto;
  modality?: ModalityDto;
  wavelength?: number;
  comment?: string;
}

export interface SaveMetadataDto {
  date: string;
  scientist: string; //uuid
  temperature: number;
  solvent: string; //uuid
  concentrationA: number;
  concentrationB: number;
  instrument?: string; //uuid
  modality?: string;
  wavelength?: number;
  comment?: string;
}
