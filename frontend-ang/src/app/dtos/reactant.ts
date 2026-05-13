//TODO: overthink interface
export interface Reactant {
  shortName: string;
  smiles: string;
  type: string;
  chemicalName?: string;
  additionalShortNames?: string[];
  casNumber?: string;
  comment?: string;
  references?: string[];
  structures?: File[];
}
