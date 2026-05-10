import { Metadata } from './metadata';
import { Reactant } from './reactant';

export interface Experiment {
  id: number;
  metadata: Metadata;
  reactants: Reactant[];
}

export interface ExperimentData {
  experimentId: number;
  measurements: Measurement[];
}

export interface Measurement {
  x: number[];
  y: number[][];
}
