import { Injectable } from '@angular/core';

declare var initRDKitModule: any;

@Injectable({
  providedIn: 'root',
})
export class RDKitService {
  private rdkitModule: any = null;
  private initPromise: Promise<any> | null = null;

  constructor() {}

  async initRDKit(): Promise<any> {
    if (this.rdkitModule) {
      return this.rdkitModule;
    }

    if (!this.initPromise) {
      this.initPromise =
        typeof initRDKitModule === 'function'
          ? initRDKitModule()
          : Promise.reject('initRDKitModule is not defined globally');
    }

    try {
      this.rdkitModule = await this.initPromise;
      return this.rdkitModule;
    } catch (error) {
      this.initPromise = null;
      console.error('Failed to initialize RDKit:', error);
      throw error;
    }
  }

  get isLoaded(): boolean {
    return !!this.rdkitModule;
  }

  validateSmiles(smiles: string): 'valid' | 'syntaxError' | 'sanitizationError' {
    if (!this.rdkitModule || !smiles) return 'valid';

    // syntax errors
    let unsanitizedMol;
    try {
      const options = JSON.stringify({ sanitize: false });
      unsanitizedMol = this.rdkitModule.get_mol(smiles, options);
      if (
        !unsanitizedMol ||
        (typeof unsanitizedMol.is_valid === 'function' && !unsanitizedMol.is_valid())
      ) {
        return 'syntaxError';
      }
    } catch (e) {
      return 'syntaxError';
    } finally {
      if (unsanitizedMol) unsanitizedMol.delete();
    }

    // sanitization errors
    let sanitizedMol;
    try {
      sanitizedMol = this.rdkitModule.get_mol(smiles);
      if (
        !sanitizedMol ||
        (typeof sanitizedMol.is_valid === 'function' && !sanitizedMol.is_valid())
      ) {
        return 'sanitizationError';
      }
    } catch (e) {
      return 'sanitizationError';
    } finally {
      if (sanitizedMol) sanitizedMol.delete();
    }

    return 'valid';
  }

  getSvgFromSmiles(smiles: string): string | null {
    if (!this.rdkitModule || !smiles) return null;

    let mol;
    try {
      mol = this.rdkitModule.get_mol(smiles);
      return mol ? mol.get_svg() : null;
    } catch (error) {
      console.warn('Failed to generate SVG for SMILES:', smiles);
      return null;
    } finally {
      if (mol) mol.delete();
    }
  }

  getMolBlockFromSmiles(smiles: string): string | null {
    if (!this.rdkitModule) return null;

    let mol;
    try {
      mol = this.rdkitModule.get_mol(smiles || '');
      return mol ? mol.get_molblock() : null;
    } catch (error) {
      console.warn('Failed to generate MolBlock for SMILES:', smiles);
      return null;
    } finally {
      if (mol) mol.delete();
    }
  }

  getSmilesFromMolBlock(molBlock: string): string | null {
    if (!this.rdkitModule || !molBlock) return null;

    let mol;
    try {
      mol = this.rdkitModule.get_mol(molBlock);
      return mol ? mol.get_smiles() : null;
    } catch (error) {
      console.warn('Failed to generate SMILES from MolBlock.');
      return null;
    } finally {
      if (mol) mol.delete();
    }
  }

  getMol(smiles: string): any {
    if (!this.rdkitModule || !smiles) return null;
    try {
      return this.rdkitModule.get_mol(smiles);
    } catch (error) {
      console.warn('Invalid SMILES for get_mol:', smiles);
      return null;
    }
  }

  getQMol(smiles: string): any {
    if (!this.rdkitModule || !smiles) return null;
    try {
      return this.rdkitModule.get_qmol(smiles);
    } catch (error) {
      console.warn('Invalid SMILES for get_qmol:', smiles);
      return null;
    }
  }

  hasSubstructureMatch(targetMol: any, querySmiles: string): boolean {
    if (!this.rdkitModule || !targetMol || !querySmiles) return false;

    let qmol;
    try {
      qmol = this.getQMol(querySmiles);
      if (!qmol) return false;

      const matchObj = JSON.parse(targetMol.get_substruct_match(qmol));
      return Object.keys(matchObj).length > 0;
    } catch (error) {
      console.error('Error during substructure match:', error);
      return false;
    } finally {
      if (qmol) qmol.delete();
    }
  }
}
