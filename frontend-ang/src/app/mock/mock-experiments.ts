import { Experiment } from '../dtos/experiment'; // Passe den Pfad zu deinen Interfaces an

export const MOCK_EXPERIMENTS: Experiment[] = [
  {
    id: 1,
    reactants: [
      { shortName: 'Sodium', smiles: '[Na]', type: 'Metal' },
      { shortName: 'Chlorine', smiles: 'Cl[Cl]', type: 'Halogen' }
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
      comment: 'Initial test'
    }
  },
  {
    id: 2,
    reactants: [
      { shortName: 'Hydrogen', smiles: '[H][H]', type: 'Gas' },
      { shortName: 'Oxygen', smiles: 'O=O', type: 'Gas' }
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
      comment: 'Exothermic'
    }
  },
  {
    id: 3,
    reactants: [
      { shortName: 'Sodium', smiles: '[Na]', type: 'Metal' },
      { shortName: 'Bromine', smiles: 'Br[Br]', type: 'Halogen' }
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
      comment: 'Control group'
    }
  },
  {
    id: 4,
    reactants: [
      { shortName: 'Carbon', smiles: '[C]', type: 'Non-metal' },
      { shortName: 'Oxygen', smiles: 'O=O', type: 'Gas' }
    ],
    metadata: {
      date: new Date('2026-05-04'),
      solvent: 'Methanol',
      scientist: 'Bohr',
      temperature: 60,
      concentrations: [2.0, 1.0],
      instrument: 'Mass Spec',
      modality: 'MS',
      comment: 'High temp test' // Keine Wellenlänge hier
    }
  },
  {
    id: 5,
    reactants: [
      { shortName: 'Potassium', smiles: '[K]', type: 'Metal' },
      { shortName: 'Iodine', smiles: 'I[I]', type: 'Halogen' }
    ],
    metadata: {
      date: new Date('2026-05-04'),
      solvent: 'Acetone',
      scientist: 'Newton',
      temperature: -10,
      concentrations: [0.1, 0.2],
      instrument: 'NMR',
      modality: '1H',
      comment: 'Low temp precipitation'
    }
  },
  {
    id: 6,
    reactants: [
      { shortName: 'Iron', smiles: '[Fe]', type: 'Metal' },
      { shortName: 'Sulfur', smiles: '[S]', type: 'Non-metal' }
    ],
    metadata: {
      date: new Date('2026-05-03'),
      solvent: 'Toluene',
      scientist: 'Mendeleev',
      temperature: 150,
      concentrations: [5.0, 5.0],
      instrument: 'Calorimeter',
      modality: 'Thermal',
      comment: 'High yield exothermic'
    }
  },
  {
    id: 7,
    reactants: [
      { shortName: 'Nitrogen', smiles: 'N#N', type: 'Gas' },
      { shortName: 'Hydrogen', smiles: '[H][H]', type: 'Gas' }
    ],
    metadata: {
      date: new Date('2026-05-05'),
      solvent: 'None',
      scientist: 'Haber',
      temperature: 450,
      concentrations: [10.0, 30.0],
      instrument: 'Flow Reactor',
      modality: 'Pressure',
      comment: 'Catalyst test 3A'
    }
  },
  {
    id: 8,
    reactants: [
      { shortName: 'Copper', smiles: '[Cu]', type: 'Metal' },
      { shortName: 'Nitric Acid', smiles: 'O[N+]([O-])=O', type: 'Acid' }
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
      comment: 'Toxic gas evolved'
    }
  },
  {
    id: 9,
    reactants: [
      { shortName: 'Salicylic Acid', smiles: 'C1=CC=C(C(=C1)C(=O)O)O', type: 'Organic' },
      { shortName: 'Acetic Anhydride', smiles: 'CC(=O)OC(=O)C', type: 'Organic' }
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
      comment: 'Aspirin synthesis'
    }
  },
  {
    id: 10,
    reactants: [
      { shortName: 'Magnesium', smiles: '[Mg]', type: 'Metal' },
      { shortName: 'Hydrochloric Acid', smiles: 'Cl', type: 'Acid' }
    ],
    metadata: {
      date: new Date('2026-05-08'),
      solvent: 'Water',
      scientist: 'Bohr',
      temperature: 35,
      concentrations: [0.5, 1.0],
      instrument: 'Eudiometer',
      modality: 'Volumetric',
      comment: 'Hydrogen gas volume tracked'
    }
  }
];
