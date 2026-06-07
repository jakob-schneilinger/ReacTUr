
export interface KnownOption {
  id: number;
  category: 'type' | 'modality' | 'instrument' | 'solvent';
  value: string;
  isActive: boolean;
}
