import { Experiment, ExperimentData, Measurement } from '../dtos/experiment';
import { Reactant } from '../dtos/reactant'; // Passe den Pfad zu deinen Interfaces an

export const MOCK_REACTANTS: Reactant[] = [
  { shortName: 'Sodium', smiles: '[Na]', type1: 'Metal', chemicalName: 'full chemical name here'},
  { shortName: 'Chlorine', smiles: 'Cl[Cl]', type1: 'Halogen' },
  { shortName: 'Hydrogen', smiles: '[H][H]', type1: 'Gas' },
  { shortName: 'Oxygen', smiles: 'O=O', type1: 'Gas' },
  { shortName: 'Ethanol', smiles: 'CCO', type1: 'Solvent' },
  { shortName: 'Phenol', smiles: 'c1ccccc1O', type1: 'Solvent' },
  { shortName: 'Bromine', smiles: 'Br[Br]', type1: 'Halogen' },
  { shortName: 'Carbon', smiles: '[C]', type1: 'Non-metal' },
  { shortName: 'Potassium', smiles: '[K]', type1: 'Metal', chemicalName: 'full chemical name here'},
  { shortName: 'Iodine', smiles: 'I[I]', type1: 'Halogen' },
  { shortName: 'Iron', smiles: '[Fe]', type1: 'Metal', chemicalName: 'full chemical name here'},
  { shortName: 'Sulfur', smiles: '[S]', type1: 'Non-metal' },
  { shortName: 'Nitrogen', smiles: 'N#N', type1: 'Gas' },
  { shortName: 'Copper', smiles: '[Cu]', type1: 'Metal', chemicalName: 'full chemical name here'},
  { shortName: 'Nitric Acid', smiles: 'O[N+]([O-])=O', type1: 'Acid' },
  {
    shortName: 'Salicylic Acid',
    smiles: 'C1=CC=C(C(=C1)C(=O)O)O',
    type1: 'Organic'
  },
  {
    shortName: 'Acetic Anhydride',
    smiles: 'CC(=O)OC(=O)C',
    type1: 'Organic'
  },
  { shortName: 'Magnesium', smiles: '[Mg]', type1: 'Metal', chemicalName: 'full chemical name here'},
  { shortName: 'Hydrochloric Acid', smiles: 'Cl', type1: 'Acid' }
];

export const MOCK_EXPERIMENTS: Experiment[] = [
  {
    id: 1,
    reactants: [
      { shortName: 'Sodium', smiles: '[Na]', type1: 'Metal', chemicalName: ' full chemical name here '},
      { shortName: 'Chlorine', smiles: 'Cl[Cl]', type1: 'Halogen' },
    ],
    metadata: {
      date: new Date('2026-05-01'),
      solvent: 'Water',
      scientist: 'Curie',
      temperature: 25,
      concentrations: [1.0, 2.0],
      instrument: 'Spectrometer',
      modality: 'UV',
      wavelength: 250,
      comment: 'Initial test',
    },
  },
  {
    id: 2,
    reactants: [
      { shortName: 'Hydrogen', smiles: '[H][H]', type1: 'Gas' },
      { shortName: 'Oxygen', smiles: 'O=O', type1: 'Gas' },
    ],
    metadata: {
      date: new Date('2026-05-02'),
      solvent: 'Ethanol',
      scientist: 'Einstein',
      temperature: 40,
      concentrations: [0.5, 0.5],
      instrument: 'Chromatograph',
      modality: 'IR',
      wavelength: 300,
      comment: 'Exothermic',
    },
  },
  {
    id: 3,
    reactants: [
      { shortName: 'Sodium', smiles: '[Na]', type1: 'Metal', chemicalName: 'full chemical name here'},
      { shortName: 'Bromine', smiles: 'Br[Br]', type1: 'Halogen' },
    ],
    metadata: {
      date: new Date('2026-05-05'),
      solvent: 'Water',
      scientist: 'Curie',
      temperature: 20,
      concentrations: [1.5, 1.0],
      instrument: 'Spectrometer',
      modality: 'UV',
      wavelength: 260,
      comment: 'Control group',
    },
  },
  {
    id: 4,
    reactants: [
      { shortName: 'Carbon', smiles: '[C]', type1: 'Non-metal' },
      { shortName: 'Oxygen', smiles: 'O=O', type1: 'Gas' },
    ],
    metadata: {
      date: new Date('2026-05-04'),
      solvent: 'Methanol',
      scientist: 'Bohr',
      temperature: 60,
      concentrations: [2.0, 1.0],
      instrument: 'Mass Spec',
      modality: 'MS',
      comment: 'High temp test', // Keine Wellenlänge hier
    },
  },
  {
    id: 5,
    reactants: [
      { shortName: 'Potassium', smiles: '[K]', type1: 'Metal', chemicalName: 'full chemical name here'},
      { shortName: 'Iodine', smiles: 'I[I]', type1: 'Halogen' },
    ],
    metadata: {
      date: new Date('2026-05-04'),
      solvent: 'Acetone',
      scientist: 'Newton',
      temperature: -10,
      concentrations: [0.1, 0.2],
      instrument: 'NMR',
      modality: '1H',
      comment: 'Low temp precipitation',
    },
  },
  {
    id: 6,
    reactants: [
      { shortName: 'Iron', smiles: '[Fe]', type1: 'Metal', chemicalName: 'full chemical name here'},
      { shortName: 'Sulfur', smiles: '[S]', type1: 'Non-metal' },
    ],
    metadata: {
      date: new Date('2026-05-03'),
      solvent: 'Toluene',
      scientist: 'Mendeleev',
      temperature: 150,
      concentrations: [5.0, 5.0],
      instrument: 'Calorimeter',
      modality: 'Thermal',
      comment: 'High yield exothermic',
    },
  },
  {
    id: 7,
    reactants: [
      { shortName: 'Nitrogen', smiles: 'N#N', type1: 'Gas' },
      { shortName: 'Hydrogen', smiles: '[H][H]', type1: 'Gas' },
    ],
    metadata: {
      date: new Date('2026-05-05'),
      solvent: 'None',
      scientist: 'Haber',
      temperature: 450,
      concentrations: [10.0, 30.0],
      instrument: 'Flow Reactor',
      modality: 'Pressure',
      comment: 'Catalyst test 3A',
    },
  },
  {
    id: 8,
    reactants: [
      { shortName: 'Copper', smiles: '[Cu]', type1: 'Metal', chemicalName: 'full chemical name here'},
      { shortName: 'Nitric Acid', smiles: 'O[N+]([O-])=O', type1: 'Acid' },
    ],
    metadata: {
      date: new Date('2026-05-06'),
      solvent: 'Water',
      scientist: 'Curie',
      temperature: 22,
      concentrations: [1.0, 6.0],
      instrument: 'Spectrometer',
      modality: 'Vis',
      wavelength: 600,
      comment: 'Toxic gas evolved',
    },
  },
  {
    id: 9,
    reactants: [
      { shortName: 'Salicylic Acid', smiles: 'C1=CC=C(C(=C1)C(=O)O)O', type1: 'Organic' },
      { shortName: 'Acetic Anhydride', smiles: 'CC(=O)OC(=O)C', type1: 'Organic' },
    ],
    metadata: {
      date: new Date('2026-05-07'),
      solvent: 'Phosphoric Acid',
      scientist: 'Hoffman',
      temperature: 90,
      concentrations: [2.5, 3.0],
      instrument: 'HPLC',
      modality: 'UV',
      wavelength: 230,
      comment: 'Aspirin synthesis',
    },
  },
  {
    id: 10,
    reactants: [
      { shortName: 'Magnesium', smiles: '[Mg]', type1: 'Metal', chemicalName: 'full chemical name here'},
      { shortName: 'Hydrochloric Acid', smiles: 'Cl', type1: 'Acid' },
    ],
    metadata: {
      date: new Date('2026-05-08'),
      solvent: 'Water',
      scientist: 'Bohr',
      temperature: 35,
      concentrations: [0.5, 1.0],
      instrument: 'Eudiometer',
      modality: 'Volumetric',
      comment: 'Hydrogen gas volume tracked',
    },
  },
];

function generateMockMeasurement(): Measurement {
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
    y3.push(0.0140 + (Math.random() * 0.0002 - 0.0010));
    y4.push(0.0137 + (Math.random() * 0.0001 - 0.0005));

    currentX += 0.0075;
  }

  return {
    x: x,
    y: [y1, y2, y3, y4]
  };
}

export const MOCK_EXPERIMENT_DATA: ExperimentData[] = MOCK_EXPERIMENTS.map(exp => ({
  experimentId: exp.id,
  measurements: [generateMockMeasurement()]
}));
