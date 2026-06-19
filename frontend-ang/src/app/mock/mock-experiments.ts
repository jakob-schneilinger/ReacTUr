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

export const MOCK_REACTANTS: ReactantDto[] = [
  {
    uuid: 'r-1',
    shortName: 'Sodium',
    smiles: '[Na]',
    chemicalType: MOCK_CHEM_TYPES.METAL,
    chemicalName: 'Sodium Metal',
    archivedAt: null,
  },
  {
    uuid: 'r-2',
    shortName: 'Chlorine',
    smiles: 'Cl[Cl]',
    chemicalType: MOCK_CHEM_TYPES.HALOGEN,
    archivedAt: null,
  },
  {
    uuid: 'r-3',
    shortName: 'Hydrogen',
    smiles: '[H][H]',
    chemicalType: MOCK_CHEM_TYPES.GAS,
    archivedAt: null,
  },
  {
    uuid: 'r-4',
    shortName: 'Oxygen',
    smiles: 'O=O',
    chemicalType: MOCK_CHEM_TYPES.GAS,
    archivedAt: null,
  },
  {
    uuid: 'r-5',
    shortName: 'Ethanol',
    smiles: 'CCO',
    chemicalType: MOCK_CHEM_TYPES.SOLVENT,
    archivedAt: null,
  },
  {
    uuid: 'r-6',
    shortName: 'Phenol',
    smiles: 'c1ccccc1O',
    chemicalType: MOCK_CHEM_TYPES.SOLVENT,
    archivedAt: null,
  },
  {
    uuid: 'r-7',
    shortName: 'Bromine',
    smiles: 'Br[Br]',
    chemicalType: MOCK_CHEM_TYPES.HALOGEN,
    archivedAt: null,
  },
  {
    uuid: 'r-8',
    shortName: 'Carbon',
    smiles: '[C]',
    chemicalType: MOCK_CHEM_TYPES.NON_METAL,
    archivedAt: null,
  },
  {
    uuid: 'r-9',
    shortName: 'Potassium',
    smiles: '[K]',
    chemicalType: MOCK_CHEM_TYPES.METAL,
    chemicalName: 'Potassium Metal',
    archivedAt: null,
  },
  {
    uuid: 'r-10',
    shortName: 'Iodine',
    smiles: 'I[I]',
    chemicalType: MOCK_CHEM_TYPES.HALOGEN,
    archivedAt: null,
  },
  {
    uuid: 'r-11',
    shortName: 'Iron',
    smiles: '[Fe]',
    chemicalType: MOCK_CHEM_TYPES.METAL,
    chemicalName: 'Iron Solid',
    archivedAt: null,
  },
  {
    uuid: 'r-12',
    shortName: 'Sulfur',
    smiles: '[S]',
    chemicalType: MOCK_CHEM_TYPES.NON_METAL,
    archivedAt: null,
  },
  {
    uuid: 'r-13',
    shortName: 'Nitrogen',
    smiles: 'N#N',
    chemicalType: MOCK_CHEM_TYPES.GAS,
    archivedAt: null,
  },
  {
    uuid: 'r-14',
    shortName: 'Copper',
    smiles: '[Cu]',
    chemicalType: MOCK_CHEM_TYPES.METAL,
    chemicalName: 'Copper Solid',
    archivedAt: null,
  },
  {
    uuid: 'r-15',
    shortName: 'Nitric Acid',
    smiles: 'O[N+]([O-])=O',
    chemicalType: MOCK_CHEM_TYPES.ACID,
    archivedAt: null,
  },
  {
    uuid: 'r-16',
    shortName: 'Salicylic Acid',
    smiles: 'C1=CC=C(C(=C1)C(=O)O)O',
    chemicalType: MOCK_CHEM_TYPES.ORGANIC,
    archivedAt: null,
  },
  {
    uuid: 'r-17',
    shortName: 'Acetic Anhydride',
    smiles: 'CC(=O)OC(=O)C',
    chemicalType: MOCK_CHEM_TYPES.ORGANIC,
    archivedAt: null,
  },
  {
    uuid: 'r-18',
    shortName: 'Magnesium',
    smiles: '[Mg]',
    chemicalType: MOCK_CHEM_TYPES.METAL,
    chemicalName: 'Magnesium Ribbons',
    archivedAt: null,
  },
  {
    uuid: 'r-19',
    shortName: 'Hydrochloric Acid',
    smiles: 'Cl',
    chemicalType: MOCK_CHEM_TYPES.ACID,
    archivedAt: null,
  },
];

export const MOCK_EXPERIMENTS: GetExperimentDto[] = [
  {
    uuid: 'exp-1',
    doi: '10.1000/xyz123',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[0], // Sodium
    reactantB: MOCK_REACTANTS[1], // Chlorine
    createdBy: MOCK_USERS.CURIE,
    metadata: {
      uuid: 'meta-1',
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
    uuid: 'exp-2',
    doi: '10.1000/xyz124',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[2], // Hydrogen
    reactantB: MOCK_REACTANTS[3], // Oxygen
    createdBy: MOCK_USERS.EINSTEIN,
    metadata: {
      uuid: 'meta-2',
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
    uuid: 'exp-3',
    doi: '10.1000/xyz125',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[0], // Sodium
    reactantB: MOCK_REACTANTS[6], // Bromine
    createdBy: MOCK_USERS.CURIE,
    metadata: {
      uuid: 'meta-3',
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
    uuid: 'exp-4',
    doi: '10.1000/xyz126',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[7], // Carbon
    reactantB: MOCK_REACTANTS[3], // Oxygen
    createdBy: MOCK_USERS.BOHR,
    metadata: {
      uuid: 'meta-4',
      archivedAt: null,
      date: '2026-05-04T11:00:00Z',
      solvent: MOCK_SOLVENTS.METHANOL,
      scientist: MOCK_USERS.BOHR,
      temperature: 60,
      concentrationA: 2.0,
      concentrationB: 1.0,
      instrument: MOCK_INSTRUMENTS.MASS_SPEC,
      modality: MOCK_MODALITIES.MS,
      comment: 'High temp test', // Keine Wellenlänge hier
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
    uuid: 'exp-5',
    doi: '10.1000/xyz127',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[8], // Potassium
    reactantB: MOCK_REACTANTS[9], // Iodine
    createdBy: MOCK_USERS.NEWTON,
    metadata: {
      uuid: 'meta-5',
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
    uuid: 'exp-6',
    doi: '10.1000/xyz128',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[10], // Iron
    reactantB: MOCK_REACTANTS[11], // Sulfur
    createdBy: MOCK_USERS.MENDELEEV,
    metadata: {
      uuid: 'meta-6',
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
    uuid: 'exp-7',
    doi: '10.1000/xyz129',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[12], // Nitrogen
    reactantB: MOCK_REACTANTS[2], // Hydrogen
    createdBy: MOCK_USERS.HABER,
    metadata: {
      uuid: 'meta-7',
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
    uuid: 'exp-8',
    doi: '10.1000/xyz130',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[13], // Copper
    reactantB: MOCK_REACTANTS[14], // Nitric Acid
    createdBy: MOCK_USERS.CURIE,
    metadata: {
      uuid: 'meta-8',
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
    uuid: 'exp-9',
    doi: '10.1000/xyz131',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[15], // Salicylic Acid
    reactantB: MOCK_REACTANTS[16], // Acetic Anhydride
    createdBy: MOCK_USERS.HOFFMAN,
    metadata: {
      uuid: 'meta-9',
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
    uuid: 'exp-10',
    doi: '10.1000/xyz132',
    archivedAt: null,
    reactantA: MOCK_REACTANTS[17], // Magnesium
    reactantB: MOCK_REACTANTS[18], // Hydrochloric Acid
    createdBy: MOCK_USERS.BOHR,
    metadata: {
      uuid: 'meta-10',
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
