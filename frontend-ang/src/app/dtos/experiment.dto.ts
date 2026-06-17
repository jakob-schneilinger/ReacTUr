import { MetadataDto, GetMetadataDto, SaveMetadataDto } from './metadata.dto';
import { ReactantDto } from './reactant.dto';
import { GetUserDto, UserDto } from './user.dto';
import { DetailsDto } from './details.dto';

// old
export interface ExperimentDto {
  id: number;
  metadata: MetadataDto;
  reactants: ReactantDto[];
}

export interface ExperimentDataDto {
  experimentId: string;
  measurements: MeasurementDto[];
}

export interface MeasurementDto {
  x: number[];
  y: number[][];
}


// new
export interface GetExperimentDto extends DetailsDto {
  doi: string;
  reactantA: ReactantDto;
  reactantB: ReactantDto;
  metadata: GetMetadataDto;
  measurementData: MeasurementDataDto;
  createdBy: GetUserDto;
  attachments: ExperimentAttachmentsDto[];
}

export interface SaveExperimentDto {
  reactantA: string; //uuid
  reactantB: string; //uuid
  metadata: SaveMetadataDto;
  measurementData: MeasurementDataDto;
  doi: string;
}

export interface ExperimentAttachmentsDto {
  fileName: string;
  fileType: string;
  fileSizeBytes: number;
  downloadUrl: string;
}

export interface MeasurementDataDto {
  pFO_rateConstant?: number;
  rateConstant: number;
  error: number;
  isErrorPercentage: boolean;
}




