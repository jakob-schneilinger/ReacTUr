import { ChemicalTypeDto, InstrumentDto, ModalityDto, SolventDto } from '../shared/dtos/selection-option.dto';
import { GetUserDto } from '../shared/dtos/user.dto';
import { Role } from '../shared/dtos/enums/role.enum';

// Mock Users
export const MOCK_USERS = {
  CURIE: {
    id: 'u-1',
    firstName: 'Marie',
    lastName: 'Curie',
    email: 'marie@lab.com',
    role: Role.ADMIN,
    deactivatedAt: null,
  },
  EINSTEIN: {
    id: 'u-2',
    firstName: 'Albert',
    lastName: 'Einstein',
    email: 'albert@lab.com',
    role: Role.SUPERVISOR,
    deactivatedAt: null,
  },
  BOHR: {
    id: 'u-3',
    firstName: 'Niels',
    lastName: 'Bohr',
    email: 'niels@lab.com',
    role: Role.USER,
    deactivatedAt: null,
  },
  NEWTON: {
    id: 'u-4',
    firstName: 'Isaac',
    lastName: 'Newton',
    email: 'isaac@lab.com',
    role: Role.USER,
    deactivatedAt: null,
  },
  MENDELEEV: {
    id: 'u-5',
    firstName: 'Dmitri',
    lastName: 'Mendeleev',
    email: 'dmitri@lab.com',
    role: Role.SUPERVISOR,
    deactivatedAt: null,
  },
  HABER: {
    id: 'u-6',
    firstName: 'Fritz',
    lastName: 'Haber',
    email: 'fritz@lab.com',
    role: Role.USER,
    deactivatedAt: null,
  },
  HOFFMAN: {
    id: 'u-7',
    firstName: 'Felix',
    lastName: 'Hoffman',
    email: 'felix@lab.com',
    role: Role.USER,
    deactivatedAt: null,
  },
  PENDING1: {
    id: 'u-8',
    firstName: 'Pending1',
    lastName: 'User1',
    email: 'pending1@user1.com',
    role: Role.UNAUTHORIZED,
    deactivatedAt: null,
  },
  PENDING2: {
    id: 'u-9',
    firstName: 'Pending2',
    lastName: 'User2',
    email: 'pending2@user2.com',
    role: Role.UNAUTHORIZED,
    deactivatedAt: null,
  },
} satisfies Record<string, GetUserDto>;

// Mock Chemical Types
export const MOCK_CHEM_TYPES = {
  METAL: { uuid: '9ed34c45-71ae-4da3-94fb-a6c5f3571fec', name: 'Metal', archivedAt: null },
  HALOGEN: { uuid: 'da81c351-1f18-4b84-a07b-7be91670ae99', name: 'Halogen', archivedAt: null },
  GAS: { uuid: 'f84cc51d-0194-4ec5-8d6f-67ec687ec586', name: 'Gas', archivedAt: null },
  SOLVENT: { uuid: 'e58e0c4c-7f1c-4893-b1b5-8538c150d9ec', name: 'Solvent', archivedAt: null },
  NON_METAL: { uuid: '8d454795-23f1-4072-a897-a0eb562e92ea', name: 'Non-metal', archivedAt: null },
  ACID: { uuid: '4167f582-de9b-4db2-8fbd-70b8951bac4c', name: 'Acid', archivedAt: null },
  ORGANIC: { uuid: '6ff9fc60-b571-40de-8355-18079c3a06d2', name: 'Organic', archivedAt: null },
} satisfies Record<string, ChemicalTypeDto>;

// Mock Solvents
export const MOCK_SOLVENTS = {
  WATER: { uuid: '0d5d8269-3923-4f65-91b1-794b01084a3e', name: 'Water', archivedAt: null },
  ETHANOL: { uuid: 'cf54e860-0746-4c2e-9ba4-eadf480ae1c1', name: 'Ethanol', archivedAt: null },
  METHANOL: { uuid: '078cd2a1-9487-4270-83b6-3c2401042d68', name: 'Methanol', archivedAt: null },
  ACETONE: { uuid: 'a3d55e6b-9251-4c0b-a232-c7d38870380d', name: 'Acetone', archivedAt: null },
  TOLUENE: { uuid: 'e0b5eadc-7114-4f9f-9d51-e2003aa94432', name: 'Toluene', archivedAt: null },
  NONE: { uuid: '3672ea23-6170-4e8e-bf78-4e0e4cec5ec5', name: 'None', archivedAt: null },
  PHOSPHORIC_ACID: {
    uuid: '3f611b07-d6fc-40e4-91fa-0dcb3a3e4410',
    name: 'Phosphoric Acid',
    archivedAt: null,
  },
} satisfies Record<string, SolventDto>;

// Mock Instruments
export const MOCK_INSTRUMENTS = {
  SPECTROMETER: {
    uuid: 'dc392466-bb29-4707-8e13-cedad16e6118',
    name: 'Spectrometer',
    archivedAt: null,
  },
  CHROMATOGRAPH: {
    uuid: '5f9beac3-2e92-41a6-b900-4aa73ae021d1',
    name: 'Chromatograph',
    archivedAt: null,
  },
  MASS_SPEC: { uuid: '5f9beac3-2e92-41a6-b900-4aa73ae021d3', name: 'Mass Spec', archivedAt: null },
  NMR: { uuid: '5f9beac3-2e92-41a6-b900-4aa73ae021d4', name: 'NMR', archivedAt: null },
  CALORIMETER: {
    uuid: '35984d8b-923e-4ef4-a189-512ef4858605',
    name: 'Calorimeter',
    archivedAt: null,
  },
  FLOW_REACTOR: {
    uuid: '35984d8b-923e-4ef4-a189-512ef4858606',
    name: 'Flow Reactor',
    archivedAt: null,
  },
  HPLC: { uuid: '35984d8b-923e-4ef4-a189-512ef4858607', name: 'HPLC', archivedAt: null },
  EUDIOMETER: {
    uuid: '35984d8b-923e-4ef4-a189-512ef4858608',
    name: 'Eudiometer',
    archivedAt: null,
  },
} satisfies Record<string, InstrumentDto>;

// Mock Modalities
export const MOCK_MODALITIES = {
  UV: { uuid: '7adf9914-93c1-4bb2-a26a-56c638061791', name: 'UV', archivedAt: null },
  IR: { uuid: '7adf9914-93c1-4bb2-a26a-56c638061792', name: 'IR', archivedAt: null },
  MS: { uuid: '7adf9914-93c1-4bb2-a26a-56c638061793', name: 'MS', archivedAt: null },
  H1: { uuid: '7adf9914-93c1-4bb2-a26a-56c638061794', name: '1H', archivedAt: null },
  THERMAL: { uuid: '7adf9914-93c1-4bb2-a26a-56c638061795', name: 'Thermal', archivedAt: null },
  PRESSURE: { uuid: '7adf9914-93c1-4bb2-a26a-56c638061796', name: 'Pressure', archivedAt: null },
  VIS: { uuid: '7adf9914-93c1-4bb2-a26a-56c638061797', name: 'Vis', archivedAt: null },
  VOLUMETRIC: {
    uuid: '7adf9914-93c1-4bb2-a26a-56c638061798',
    name: 'Volumetric',
    archivedAt: null,
  },
} satisfies Record<string, ModalityDto>;
