import { DetailsDto } from './details.dto';

// old
export interface KnownOptionDto {
  id: number;
  category: 'type' | 'modality' | 'instrument' | 'solvent';
  value: string;
  isActive: boolean;
}

// new
export interface SelectionOptionDto extends DetailsDto {
  name: string;
}

export interface ChemicalTypeDto extends SelectionOptionDto {}

export interface InstrumentDto extends SelectionOptionDto {}

export interface ModalityDto extends SelectionOptionDto {}

export interface SolventDto extends SelectionOptionDto {}
