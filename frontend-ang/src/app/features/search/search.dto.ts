
export interface MeasurementSearchDto {
  pFO_rateConstantMin?: number;
  pFO_rateConstantMax?: number;
  rateConstantMin?: number;
  rateConstantMax?: number;
}

export interface SearchParametersDto {
  reactantA: string[]; //uuid
  reactantB: string[]; //uuid
  dateMin?: string;
  dateMax?: string;
  scientist: string[]; //uuid
  temperature_Min?: number;
  temperature_Max?: number;
  solvent?: string; //uuid
  concentrationA_Min?: number;
  concentrationA_Max?: number;
  concentrationB_Min?: number;
  concentrationB_Max?: number;
  instrument?: string; //uuid
  modality?: string; //uuid
  wavelength_Min?: number;
  wavelength_Max?: number;
  comment?: string;
  measurementSearch?: MeasurementSearchDto;
}
