//TODO: overthink interface
export interface Metadata {
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

export interface AdvancedSearch {
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
