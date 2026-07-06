import { MetadataDto, GetMetadataDto, SaveMetadataDto } from '../../shared/dtos/metadata.dto';
import { ReactantDto } from '../reactants/reactant.dto';
import { GetUserDto } from '../../shared/dtos/user.dto';
import { DetailsDto } from '../../shared/dtos/details.dto';

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
  attachments: ExperimentAttachmentDto[];
}

export interface SaveExperimentDto {
  reactantA: string; //uuid
  reactantB: string; //uuid
  metadata: SaveMetadataDto;
  measurementData: MeasurementDataDto;
  doi: string;
}

export interface ExperimentAttachmentDto {
  uuid: string;
  fileName: string;
  fileType: string;
  fileSizeBytes: number;
  downloadUrl: string;
  checksum?: string;
}

export interface MeasurementDataDto {
  pFO_rateConstant?: number;
  rateConstant: number;
  error: number;
  isErrorPercentage: boolean;
}




