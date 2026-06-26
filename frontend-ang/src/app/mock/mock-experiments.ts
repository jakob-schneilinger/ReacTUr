import {
  ExperimentDataDto,
  MeasurementDto,
  GetExperimentDto,
} from '../dtos/experiment.dto';
import { ReactantDto } from '../dtos/reactant.dto';
import {
  MOCK_CHEM_TYPES,
  MOCK_INSTRUMENTS,
  MOCK_MODALITIES,
  MOCK_SOLVENTS,
  MOCK_USERS,
} from './mock.data';

import { MOCK_REACTANTS } from './mock-reactants';

export const MOCK_EXPERIMENTS: GetExperimentDto[] = [
  {
    uuid: '5d955cec-c2c1-4551-8070-07c5717505c8',
    doi: '10.1000/xyz123',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[0],
    reactantB: MOCK_REACTANTS[1],
    createdBy: MOCK_USERS.CURIE,
    metadata: {
      uuid: '6667ff0b-761a-4f24-94d3-c286a2d23aa4',
      archivedAt: null,
      date: '2026-05-01T08:00:00Z',
      solvent: MOCK_SOLVENTS.WATER,
      scientist: MOCK_USERS.CURIE,
      temperature: 25,
      concentrationA: 1.0,
      concentrationB: 2.0,
      instrument: MOCK_INSTRUMENTS.SPECTROMETER,
      modality: MOCK_MODALITIES.UV,
      wavelength: 250,
      comment: 'Initial test',
    },
    measurementData: { rateConstant: 0.45, error: 0.05, isErrorPercentage: false },
    attachments: [
      {
        fileName: 'data1.csv',
        fileType: 'text/csv',
        fileSizeBytes: 1024,
        downloadUrl: '/api/files/1',
      },
    ],
  },
  {
    uuid: '0ca74037-956d-4dda-9f28-34153859e21c',
    doi: '10.1000/xyz124',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[1],
    reactantB: MOCK_REACTANTS[2],
    createdBy: MOCK_USERS.EINSTEIN,
    metadata: {
      uuid: 'fbd398e9-c044-4b36-b6e4-f33623e6c3ce',
      archivedAt: null,
      date: '2026-05-02T09:30:00Z',
      solvent: MOCK_SOLVENTS.ETHANOL,
      scientist: MOCK_USERS.EINSTEIN,
      temperature: 40,
      concentrationA: 0.5,
      concentrationB: 0.5,
      instrument: MOCK_INSTRUMENTS.CHROMATOGRAPH,
      modality: MOCK_MODALITIES.IR,
      wavelength: 300,
      comment: 'Exothermic',
    },
    measurementData: { rateConstant: 1.2, error: 2.5, isErrorPercentage: true },
    attachments: [
      {
        fileName: 'data2.csv',
        fileType: 'text/csv',
        fileSizeBytes: 2048,
        downloadUrl: '/api/files/2',
      },
    ],
  },
  {
    uuid: '64c4d973-6416-4a24-8496-96d6d6005a5c',
    doi: '10.1000/xyz125',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[3],
    reactantB: MOCK_REACTANTS[4],
    createdBy: MOCK_USERS.CURIE,
    metadata: {
      uuid: 'b8fc9534-8a1d-413c-8451-1857d3051821',
      archivedAt: null,
      date: '2026-05-05T10:15:00Z',
      solvent: MOCK_SOLVENTS.WATER,
      scientist: MOCK_USERS.CURIE,
      temperature: 20,
      concentrationA: 1.5,
      concentrationB: 1.0,
      instrument: MOCK_INSTRUMENTS.SPECTROMETER,
      modality: MOCK_MODALITIES.UV,
      wavelength: 260,
      comment: 'Control group',
    },
    measurementData: { rateConstant: 0.35, error: 0.02, isErrorPercentage: false },
    attachments: [
      {
        fileName: 'data3.csv',
        fileType: 'text/csv',
        fileSizeBytes: 1500,
        downloadUrl: '/api/files/3',
      },
    ],
  },
  {
    uuid: 'e34cfcef-539e-4d71-abd3-be06c70a7f2c',
    doi: '10.1000/xyz126',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[4],
    reactantB: MOCK_REACTANTS[5],
    createdBy: MOCK_USERS.BOHR,
    metadata: {
      uuid: '799a53db-eda8-4447-8278-6e44bbb9a66d',
      archivedAt: null,
      date: '2026-05-04T11:00:00Z',
      solvent: MOCK_SOLVENTS.METHANOL,
      scientist: MOCK_USERS.BOHR,
      temperature: 60,
      concentrationA: 2.0,
      concentrationB: 1.0,
      instrument: MOCK_INSTRUMENTS.MASS_SPEC,
      modality: MOCK_MODALITIES.MS,
      comment: 'High temp test',
    },
    measurementData: { rateConstant: 5.0, error: 0.1, isErrorPercentage: false },
    attachments: [
      {
        fileName: 'ms_spectra.mzML',
        fileType: 'application/xml',
        fileSizeBytes: 45000,
        downloadUrl: '/api/files/4',
      },
    ],
  },
  {
    uuid: 'e2f35ba3-1ae7-4c58-9b2a-419237eb8f9e',
    doi: '10.1000/xyz127',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[5],
    reactantB: MOCK_REACTANTS[6],
    createdBy: MOCK_USERS.NEWTON,
    metadata: {
      uuid: '308fe5c5-3cca-469f-b55c-60b098f34bd7',
      archivedAt: null,
      date: '2026-05-04T14:45:00Z',
      solvent: MOCK_SOLVENTS.ACETONE,
      scientist: MOCK_USERS.NEWTON,
      temperature: -10,
      concentrationA: 0.1,
      concentrationB: 0.2,
      instrument: MOCK_INSTRUMENTS.NMR,
      modality: MOCK_MODALITIES.H1,
      comment: 'Low temp precipitation',
    },
    measurementData: { rateConstant: 0.05, error: 5.0, isErrorPercentage: true },
    attachments: [
      {
        fileName: 'nmr_fid.raw',
        fileType: 'application/octet-stream',
        fileSizeBytes: 12000,
        downloadUrl: '/api/files/5',
      },
    ],
  },
  {
    uuid: '5e1ffe0c-a4cd-4b15-909d-1b65521a7227',
    doi: '10.1000/xyz128',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[6],
    reactantB: MOCK_REACTANTS[7],
    createdBy: MOCK_USERS.MENDELEEV,
    metadata: {
      uuid: '4842618b-074b-4863-bfbf-6fd8069fdc71',
      archivedAt: null,
      date: '2026-05-03T08:30:00Z',
      solvent: MOCK_SOLVENTS.TOLUENE,
      scientist: MOCK_USERS.MENDELEEV,
      temperature: 150,
      concentrationA: 5.0,
      concentrationB: 5.0,
      instrument: MOCK_INSTRUMENTS.CALORIMETER,
      modality: MOCK_MODALITIES.THERMAL,
      comment: 'High yield exothermic',
    },
    measurementData: { rateConstant: 8.5, error: 0.4, isErrorPercentage: false },
    attachments: [
      {
        fileName: 'thermogram.csv',
        fileType: 'text/csv',
        fileSizeBytes: 3000,
        downloadUrl: '/api/files/6',
      },
    ],
  },
  {
    uuid: '5ea69c1a-3c56-4340-ae02-6a0e832dde11',
    doi: '10.1000/xyz129',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[7],
    reactantB: MOCK_REACTANTS[8],
    createdBy: MOCK_USERS.HABER,
    metadata: {
      uuid: '5927b88a-cf90-4b02-aeb6-8b26279bdaeb',
      archivedAt: null,
      date: '2026-05-05T16:00:00Z',
      solvent: MOCK_SOLVENTS.NONE,
      scientist: MOCK_USERS.HABER,
      temperature: 450,
      concentrationA: 10.0,
      concentrationB: 30.0,
      instrument: MOCK_INSTRUMENTS.FLOW_REACTOR,
      modality: MOCK_MODALITIES.PRESSURE,
      comment: 'Catalyst test 3A',
    },
    measurementData: { rateConstant: 12.0, error: 1.0, isErrorPercentage: true },
    attachments: [
      {
        fileName: 'flow_log.txt',
        fileType: 'text/plain',
        fileSizeBytes: 800,
        downloadUrl: '/api/files/7',
      },
    ],
  },
  {
    uuid: '71f44860-e583-443f-95a6-e07f7cac3f4a',
    doi: '10.1000/xyz130',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[8],
    reactantB: MOCK_REACTANTS[9],
    createdBy: MOCK_USERS.CURIE,
    metadata: {
      uuid: '164645ad-b755-49ab-a188-d7c9e03ab7b6',
      archivedAt: null,
      date: '2026-05-06T09:00:00Z',
      solvent: MOCK_SOLVENTS.WATER,
      scientist: MOCK_USERS.CURIE,
      temperature: 22,
      concentrationA: 1.0,
      concentrationB: 6.0,
      instrument: MOCK_INSTRUMENTS.SPECTROMETER,
      modality: MOCK_MODALITIES.VIS,
      wavelength: 600,
      comment: 'Toxic gas evolved',
    },
    measurementData: { rateConstant: 2.1, error: 0.15, isErrorPercentage: false },
    attachments: [
      {
        fileName: 'spectra_vis.csv',
        fileType: 'text/csv',
        fileSizeBytes: 2200,
        downloadUrl: '/api/files/8',
      },
    ],
  },
  {
    uuid: 'e645f909-3d52-4be1-add2-a612af285b38',
    doi: '10.1000/xyz131',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[9],
    reactantB: MOCK_REACTANTS[0],
    createdBy: MOCK_USERS.HOFFMAN,
    metadata: {
      uuid: '249be66b-00e9-4471-81bd-f3fc6da5b467',
      archivedAt: null,
      date: '2026-05-07T13:20:00Z',
      solvent: MOCK_SOLVENTS.PHOSPHORIC_ACID,
      scientist: MOCK_USERS.HOFFMAN,
      temperature: 90,
      concentrationA: 2.5,
      concentrationB: 3.0,
      instrument: MOCK_INSTRUMENTS.HPLC,
      modality: MOCK_MODALITIES.UV,
      wavelength: 230,
      comment: 'Aspirin synthesis',
    },
    measurementData: { rateConstant: 0.8, error: 2.0, isErrorPercentage: true },
    attachments: [
      {
        fileName: 'chromatogram.pdf',
        fileType: 'application/pdf',
        fileSizeBytes: 105000,
        downloadUrl: '/api/files/9',
      },
    ],
  },
  {
    uuid: 'a594e746-9426-4cee-947e-e3057bd64d58',
    doi: '10.1000/xyz132',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[1],
    reactantB: MOCK_REACTANTS[0],
    createdBy: MOCK_USERS.BOHR,
    metadata: {
      uuid: 'e1d6869d-d140-48dd-9f12-066947cb11b3',
      archivedAt: null,
      date: '2026-05-08T10:00:00Z',
      solvent: MOCK_SOLVENTS.WATER,
      scientist: MOCK_USERS.BOHR,
      temperature: 35,
      concentrationA: 0.5,
      concentrationB: 1.0,
      instrument: MOCK_INSTRUMENTS.EUDIOMETER,
      modality: MOCK_MODALITIES.VOLUMETRIC,
      comment: 'Hydrogen gas volume tracked',
    },
    measurementData: { rateConstant: 3.4, error: 0.2, isErrorPercentage: false },
    attachments: [
      {
        fileName: 'volume_data.csv',
        fileType: 'text/csv',
        fileSizeBytes: 1800,
        downloadUrl: '/api/files/10',
      },
    ],
  },
];


function generateMockMeasurement(): MeasurementDto {
  const x: number[] = [];
  const y1: number[] = [];
  const y2: number[] = [];
  const y3: number[] = [];
  const y4: number[] = [];

  let currentX = 1.005;
  for (let i = 0; i < 100; i++) {
    x.push(parseFloat(currentX.toFixed(4)));

    y1.push(0.0137 + (Math.random() * 0.0001 - 0.0005));
    y2.push(0.0138 + (Math.random() * 0.0001 - 0.0005));
    y3.push(0.014 + (Math.random() * 0.0002 - 0.001));
    y4.push(0.0137 + (Math.random() * 0.0001 - 0.0005));

    currentX += 0.0075;
  }

  return {
    x: x,
    y: [y1, y2, y3, y4]
  };
}

export const MOCK_EXPERIMENT_DATA: ExperimentDataDto[] = MOCK_EXPERIMENTS.map((exp) => ({
  experimentId: exp.uuid,
  measurements: [generateMockMeasurement()],
}));
