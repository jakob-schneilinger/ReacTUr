import { Metadata } from './metadata';
import { Reactant } from './reactant';

export interface Experiment {
  id: number;
  metadata: Metadata;
  reactants: Reactant[];
}
