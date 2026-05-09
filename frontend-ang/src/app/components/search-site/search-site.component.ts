import { Component } from "@angular/core";
import { AdvancedSearch } from '../../dtos/metadata';
import { MOCK_EXPERIMENTS } from '../../mock/mock-experiments';
import { Experiment } from '../../dtos/experiment';

@Component({
  selector: 'app-search-site',
  templateUrl: './search-site.component.html',
  styleUrls: ['./search-site.component.scss'],
  standalone: false,
})
export class SearchSiteComponent {
  currentSearchState: 'initial' | 'no-results' | 'has-results' = 'initial';
  foundExperiments: any[] = [];

  clearSignal: number = 0;

  reactant1: string = '';
  reactant2: string = '';
  metadata: AdvancedSearch = {};

  isAdvancedSearchOpen: boolean = false;

  toggleAdvancedSearch() {
    this.isAdvancedSearchOpen = !this.isAdvancedSearchOpen;
  }

  onClearClicked() {
    this.clearSignal = Date.now();
  }

  handleSearch(terms: { reactant1: string; reactant2: string }) {
    this.reactant1 = terms.reactant1;
    this.reactant2 = terms.reactant2;
    console.log(terms); //TODO: remove when debugging done
    console.log(this.metadata); //TODO: remove when debugging done
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
        if (md.concentrationA && meta.concentrations[0]?.toString() !== md.concentrationA) return false;
        if (md.concentrationB && meta.concentrations[1]?.toString() !== md.concentrationB) return false;

        // Wellenlänge checken (optionales Feld)
        if (md.wavelength && meta.wavelength?.toString() !== md.wavelength) return false;

        // String Checks
        if (md.scientist && !meta.scientist.toLowerCase().includes(md.scientist.toLowerCase())) return false;
        if (md.solvent && !meta.solvent.toLowerCase().includes(md.solvent.toLowerCase())) return false;

        // Optionale Strings checken
        if (md.instrument && (!meta.instrument || !meta.instrument.toLowerCase().includes(md.instrument.toLowerCase()))) return false;
        if (md.modality && (!meta.modality || !meta.modality.toLowerCase().includes(md.modality.toLowerCase()))) return false;
        if (md.comment && (!meta.comment || !meta.comment.toLowerCase().includes(md.comment.toLowerCase()))) return false;

        // Temperatur Intervalle
        if (md.tempFrom && meta.temperature < parseFloat(md.tempFrom)) return false;
        if (md.tempTo && meta.temperature > parseFloat(md.tempTo)) return false;
      }

      return true;
    });
  }
}
