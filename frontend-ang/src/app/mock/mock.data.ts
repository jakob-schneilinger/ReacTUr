import { ChemicalTypeDto, InstrumentDto, ModalityDto, SolventDto } from '../dtos/selection-option.dto';
import { GetUserDto } from '../dtos/user.dto';
import { Role } from '../dtos/enums/role.enum';

// Mock Users
export const MOCK_USERS = {
  CURIE: {
    uuid: 'u-1',
    firstName: 'Marie',
    lastName: 'Curie',
    email: 'marie@lab.com',
    role: Role.ADMIN,
    deactivatedAt: null,
  },
  EINSTEIN: {
    uuid: 'u-2',
    firstName: 'Albert',
    lastName: 'Einstein',
    email: 'albert@lab.com',
    role: Role.SUPERVISOR,
    deactivatedAt: null,
  },
  BOHR: {
    uuid: 'u-3',
    firstName: 'Niels',
    lastName: 'Bohr',
    email: 'niels@lab.com',
    role: Role.USER,
    deactivatedAt: null,
  },
  NEWTON: {
    uuid: 'u-4',
    firstName: 'Isaac',
    lastName: 'Newton',
    email: 'isaac@lab.com',
    role: Role.USER,
    deactivatedAt: null,
  },
  MENDELEEV: {
    uuid: 'u-5',
    firstName: 'Dmitri',
    lastName: 'Mendeleev',
    email: 'dmitri@lab.com',
    role: Role.SUPERVISOR,
    deactivatedAt: null,
  },
  HABER: {
    uuid: 'u-6',
    firstName: 'Fritz',
    lastName: 'Haber',
    email: 'fritz@lab.com',
    role: Role.USER,
    deactivatedAt: null,
  },
  HOFFMAN: {
    uuid: 'u-7',
    firstName: 'Felix',
    lastName: 'Hoffman',
    email: 'felix@lab.com',
    role: Role.USER,
    deactivatedAt: null,
  },
  PENDING1: {
    uuid: 'u-8',
    firstName: 'Pending1',
    lastName: 'User1',
    email: 'pending1@user1.com',
    role: Role.UNAUTHORIZED,
    deactivatedAt: null,
  },
  PENDING2: {
    uuid: 'u-9',
    firstName: 'Pending2',
    lastName: 'User2',
    email: 'pending2@user2.com',
    role: Role.UNAUTHORIZED,
    deactivatedAt: null,
  },
} satisfies Record<string, GetUserDto>;

// Mock Chemical Types
export const MOCK_CHEM_TYPES= {
  METAL: { uuid: 'ct-1', name: 'Metal', archivedAt: null },
  HALOGEN: { uuid: 'ct-2', name: 'Halogen', archivedAt: null },
  GAS: { uuid: 'ct-3', name: 'Gas', archivedAt: null },
  SOLVENT: { uuid: 'ct-4', name: 'Solvent', archivedAt: null },
  NON_METAL: { uuid: 'ct-5', name: 'Non-metal', archivedAt: null },
  ACID: { uuid: 'ct-6', name: 'Acid', archivedAt: null },
  ORGANIC: { uuid: 'ct-7', name: 'Organic', archivedAt: null },
} satisfies Record<string, ChemicalTypeDto>;

// Mock Solvents
export const MOCK_SOLVENTS = {
  WATER: { uuid: 'sol-1', name: 'Water', archivedAt: null },
  ETHANOL: { uuid: 'sol-2', name: 'Ethanol', archivedAt: null },
  METHANOL: { uuid: 'sol-3', name: 'Methanol', archivedAt: null },
  ACETONE: { uuid: 'sol-4', name: 'Acetone', archivedAt: null },
  TOLUENE: { uuid: 'sol-5', name: 'Toluene', archivedAt: null },
  NONE: { uuid: 'sol-6', name: 'None', archivedAt: null },
  PHOSPHORIC_ACID: { uuid: 'sol-7', name: 'Phosphoric Acid', archivedAt: null },
} satisfies Record<string, SolventDto>;

// Mock Instruments
export const MOCK_INSTRUMENTS = {
  SPECTROMETER: { uuid: 'inst-1', name: 'Spectrometer', archivedAt: null },
  CHROMATOGRAPH: { uuid: 'inst-2', name: 'Chromatograph', archivedAt: null },
  MASS_SPEC: { uuid: 'inst-3', name: 'Mass Spec', archivedAt: null },
  NMR: { uuid: 'inst-4', name: 'NMR', archivedAt: null },
  CALORIMETER: { uuid: 'inst-5', name: 'Calorimeter', archivedAt: null },
  FLOW_REACTOR: { uuid: 'inst-6', name: 'Flow Reactor', archivedAt: null },
  HPLC: { uuid: 'inst-7', name: 'HPLC', archivedAt: null },
  EUDIOMETER: { uuid: 'inst-8', name: 'Eudiometer', archivedAt: null },
} satisfies Record<string, InstrumentDto>;

// Mock Modalities
export const MOCK_MODALITIES = {
  UV: { uuid: 'mod-1', name: 'UV', archivedAt: null },
  IR: { uuid: 'mod-2', name: 'IR', archivedAt: null },
  MS: { uuid: 'mod-3', name: 'MS', archivedAt: null },
  H1: { uuid: 'mod-4', name: '1H', archivedAt: null },
  THERMAL: { uuid: 'mod-5', name: 'Thermal', archivedAt: null },
  PRESSURE: { uuid: 'mod-6', name: 'Pressure', archivedAt: null },
  VIS: { uuid: 'mod-7', name: 'Vis', archivedAt: null },
  VOLUMETRIC: { uuid: 'mod-8', name: 'Volumetric', archivedAt: null },
} satisfies Record<string, ModalityDto>;
