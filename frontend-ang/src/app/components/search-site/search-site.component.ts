import { AfterViewInit, Component, OnInit } from '@angular/core';
import { AdvancedSearch } from '../../dtos/metadata';
import { MOCK_EXPERIMENTS } from '../../mock/mock-experiments';
import { Experiment } from '../../dtos/experiment';

declare var initRDKitModule: any;
declare var ChemDoodle: any;

@Component({
  selector: 'app-search-site',
  templateUrl: './search-site.component.html',
  styleUrls: ['./search-site.component.scss'],
  standalone: false,
})
export class SearchSiteComponent implements OnInit, AfterViewInit {
  currentSearchState: 'initial' | 'no-results' | 'has-results' = 'initial';
  foundExperiments: any[] = [];

  rdkitModule: any;
  sketcher1: any;
  sketcher2: any;

  private isInternalChange = false; // Flag to prevent infinite sync loops

  async ngOnInit() {
    this.rdkitModule = await initRDKitModule();
  }

  ngAfterViewInit() {
    // Initialize Canvases
    this.sketcher1 = new ChemDoodle.SketcherCanvas('sketcher1', 400, 250, { useServices: false });
    this.sketcher2 = new ChemDoodle.SketcherCanvas('sketcher2', 400, 250, { useServices: false });

    // Listen for drawing actions (Mouse release)
    const c1 = document.getElementById('sketcher1');
    const c2 = document.getElementById('sketcher2');

    console.log('Test1');

    c1?.addEventListener('mouseup', () => this.syncCanvasToSmiles(1));
    c2?.addEventListener('mouseup', () => this.syncCanvasToSmiles(2));
  }

  // Path 1: Visual Sketcher -> SMILES String
  syncCanvasToSmiles(num: number) {
    console.log('Test2');
    this.isInternalChange = true; // Block handleSearch from overwriting the canvas
    const sketcher = num === 1 ? this.sketcher1 : this.sketcher2;
    const molBlock = ChemDoodle.writeMOL(sketcher.getMolecule());

    try {
      const mol = this.rdkitModule.get_mol(molBlock);
      const smiles = mol.get_smiles();

      if (num === 1) this.reactant1 = smiles;
      else this.reactant2 = smiles;

      mol.delete();
      this.executeSearch();
    } catch (e) {
      /* Structure might be empty or invalid */
    }

    // Reset flag after Angular cycle
    setTimeout(() => (this.isInternalChange = false), 100);
  }

  private updateCanvasFromSmiles(num: number, smiles: string) {
    if (!this.rdkitModule || !smiles) return;
    try {
      const mol = this.rdkitModule.get_mol(smiles);
      if (mol) {
        const molBlock = mol.get_molblock();
        const sketcher = num === 1 ? this.sketcher1 : this.sketcher2;
        sketcher.loadMolecule(ChemDoodle.readMOL(molBlock));
        mol.delete();
      }
    } catch (e) {
      /* Ignore partial SMILES errors while typing */
    }
  }

  clearSignal: number = 0;

  reactant1: string = '';
  reactant2: string = '';
  metadata: AdvancedSearch = {};

  isAdvancedSearchOpen: boolean = false;

  isExportModalOpen: boolean = false;
  experimentsToExport: Experiment[] = [];

  toggleAdvancedSearch() {
    this.isAdvancedSearchOpen = !this.isAdvancedSearchOpen;
  }

  onClearClicked() {
    this.clearSignal = Date.now();
    this.reactant1 = '';
    this.reactant2 = '';
    const mol = this.rdkitModule.get_mol('');
    const molBlock = mol.get_molblock();
    this.sketcher1.loadMolecule(molBlock);
    this.sketcher2.loadMolecule(molBlock);
    this.executeSearch();
  }

  handleSearch(terms: { reactant1: string; reactant2: string }) {
    this.reactant1 = terms.reactant1;
    this.reactant2 = terms.reactant2;

    if (!this.isInternalChange) {
      this.updateCanvasFromSmiles(1, this.reactant1);
      this.updateCanvasFromSmiles(2, this.reactant2);
    }
    this.executeSearch();
  }

  handleAdvancedSearch(terms: {}) {
    this.metadata = terms;
    this.executeSearch();
  }

  handleSearchButton() {
    // TODO: implement if Search Button needed (if search on input not feasible)
    this.executeSearch();
  }

  private executeSearch() {
    const isBasicEmpty = !this.reactant1 && !this.reactant2;
    const isAdvancedEmpty =
      !this.metadata ||
      Object.values(this.metadata).every((val) => val === undefined || val === null || val === '');

    console.log(isBasicEmpty); //TODO: remove when debugging done
    console.log(isAdvancedEmpty); //TODO: remove when debugging done

    if (isBasicEmpty && isAdvancedEmpty) {
      this.currentSearchState = 'initial';
      this.foundExperiments = [];
      return;
    }

    const results = this.performMockSearch();

    if (results.length === 0) {
      this.currentSearchState = 'no-results';
      this.foundExperiments = [];
    } else {
      this.currentSearchState = 'has-results';
      this.foundExperiments = results;
    }
  }

  private performMockSearch(): Experiment[] {
    if (this.reactant1.toLowerCase() === 'x' || this.reactant2.toLowerCase() === 'x') return [];

    return MOCK_EXPERIMENTS.filter((experiment) => {
      // 1. Basic Reactant Check (Mapped zu Index 0 und 1)
      const r1Name = experiment.reactants[0]?.shortName || '';
      const r2Name = experiment.reactants[1]?.shortName || '';

      if (this.reactant1 && !r1Name.toLowerCase().includes(this.reactant1.toLowerCase())) {
        return false;
      }
      if (this.reactant2 && !r2Name.toLowerCase().includes(this.reactant2.toLowerCase())) {
        return false;
      }

      // 2. Advanced Metadata Check
      if (this.metadata) {
        const md = this.metadata;
        const meta = experiment.metadata;

        // Datum extrahieren und formatieren für Vergleich
        const expYear = meta.date.getFullYear().toString();
        // padStart fügt führende Nullen hinzu (z.B. '5' -> '05'), falls dein Filter das nutzt
        const expMonth = (meta.date.getMonth() + 1).toString().padStart(2, '0');
        const expDay = meta.date.getDate().toString().padStart(2, '0');

        if (md.dateDay && expDay !== md.dateDay.padStart(2, '0')) return false;
        if (md.dateMonth && expMonth !== md.dateMonth.padStart(2, '0')) return false;
        if (md.dateYear && expYear !== md.dateYear) return false;

        // Konzentrationen checken
        if (md.concentrationA && meta.concentrations[0]?.toString() !== md.concentrationA)
          return false;
        if (md.concentrationB && meta.concentrations[1]?.toString() !== md.concentrationB)
          return false;

        // Wellenlänge checken (optionales Feld)
        if (md.wavelength && meta.wavelength?.toString() !== md.wavelength) return false;

        // String Checks
        if (md.scientist && !meta.scientist.toLowerCase().includes(md.scientist.toLowerCase()))
          return false;
        if (md.solvent && !meta.solvent.toLowerCase().includes(md.solvent.toLowerCase()))
          return false;

        // Optionale Strings checken
        if (
          md.instrument &&
          (!meta.instrument || !meta.instrument.toLowerCase().includes(md.instrument.toLowerCase()))
        )
          return false;
        if (
          md.modality &&
          (!meta.modality || !meta.modality.toLowerCase().includes(md.modality.toLowerCase()))
        )
          return false;
        if (
          md.comment &&
          (!meta.comment || !meta.comment.toLowerCase().includes(md.comment.toLowerCase()))
        )
          return false;

        // Temperatur Intervalle
        if (md.tempFrom && meta.temperature < parseFloat(md.tempFrom)) return false;
        if (md.tempTo && meta.temperature > parseFloat(md.tempTo)) return false;
      }

      return true;
    });
  }

  handleExport(selectedIds: number[]) {
    this.experimentsToExport = this.foundExperiments.filter((exp) => selectedIds.includes(exp.id));
    this.isExportModalOpen = true;
  }
}
