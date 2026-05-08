import { Component } from "@angular/core";
import { AdvancedSearch } from '../../dtos/metadata';

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

  private performMockSearch() {
    // Extended mock database to support advanced filtering
    const mockDatabase = [
      {
        id: 1,
        reactant1: 'Sodium',
        reactant2: 'Chlorine',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '01',
        solvent: 'Water',
        scientist: 'Curie',
        temp: 25,
        concentrationA: '1.0',
        concentrationB: '2.0',
        instrument: 'Spectrometer',
        modality: 'UV',
        wavelength: '250',
        comment: 'Initial test',
      },
      {
        id: 2,
        reactant1: 'Hydrogen',
        reactant2: 'Oxygen',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '02',
        solvent: 'Ethanol',
        scientist: 'Einstein',
        temp: 40,
        concentrationA: '0.5',
        concentrationB: '0.5',
        instrument: 'Chromatograph',
        modality: 'IR',
        wavelength: '300',
        comment: 'Exothermic',
      },
      {
        id: 3,
        reactant1: 'Sodium',
        reactant2: 'Bromine',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '05',
        solvent: 'Water',
        scientist: 'Curie',
        temp: 20,
        concentrationA: '1.5',
        concentrationB: '1.0',
        instrument: 'Spectrometer',
        modality: 'UV',
        wavelength: '260',
        comment: 'Control group',
      },
      {
        id: 4,
        reactant1: 'Carbon',
        reactant2: 'Oxygen',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '04',
        solvent: 'Methanol',
        scientist: 'Bohr',
        temp: 60,
        concentrationA: '2.0',
        concentrationB: '1.0',
        instrument: 'Mass Spec',
        modality: 'MS',
        comment: 'High temp test',
      },
      {
        id: 5,
        reactant1: 'Potassium',
        reactant2: 'Iodine',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '04',
        solvent: 'Acetone',
        scientist: 'Newton',
        temp: -10,
        concentrationA: '0.1',
        concentrationB: '0.2',
        instrument: 'NMR',
        modality: '1H',
        wavelength: 'N/A',
        comment: 'Low temp precipitation',
      },
      {
        id: 6,
        reactant1: 'Iron',
        reactant2: 'Sulfur',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '03',
        solvent: 'Toluene',
        scientist: 'Mendeleev',
        temp: 150,
        concentrationA: '5.0',
        concentrationB: '5.0',
        instrument: 'Calorimeter',
        modality: 'Thermal',
        wavelength: 'N/A',
        comment: 'High yield exothermic',
      },
      {
        id: 7,
        reactant1: 'Nitrogen',
        reactant2: 'Hydrogen',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '05',
        solvent: 'None',
        scientist: 'Haber',
        temp: 450,
        concentrationA: '10.0',
        concentrationB: '30.0',
        instrument: 'Flow Reactor',
        modality: 'Pressure',
        wavelength: 'N/A',
        comment: 'Catalyst test 3A',
      },
      {
        id: 8,
        reactant1: 'Copper',
        reactant2: 'Nitric Acid',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '06',
        solvent: 'Water',
        scientist: 'Curie',
        temp: 22,
        concentrationA: '1.0',
        concentrationB: '6.0',
        instrument: 'Spectrometer',
        modality: 'Vis',
        wavelength: '600',
        comment: 'Toxic gas evolved',
      },
      {
        id: 9,
        reactant1: 'Salicylic Acid',
        reactant2: 'Acetic Anhydride',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '07',
        solvent: 'Phosphoric Acid',
        scientist: 'Hoffman',
        temp: 90,
        concentrationA: '2.5',
        concentrationB: '3.0',
        instrument: 'HPLC',
        modality: 'UV',
        wavelength: '230',
        comment: 'Aspirin synthesis',
      },
      {
        id: 10,
        reactant1: 'Magnesium',
        reactant2: 'Hydrochloric Acid',
        dateYear: '2026',
        dateMonth: '05',
        dateDay: '08',
        solvent: 'Water',
        scientist: 'Bohr',
        temp: 35,
        concentrationA: '0.5',
        concentrationB: '1.0',
        instrument: 'Eudiometer',
        modality: 'Volumetric',
        wavelength: 'N/A',
        comment: 'Hydrogen gas volume tracked',
      },
    ];

    // Catch-all mock logic from original code
    if (this.reactant1.toLowerCase() === 'x' || this.reactant2.toLowerCase() === 'x') return [];

    return mockDatabase.filter((experiment) => {
      if (
        this.reactant1 &&
        !experiment.reactant1.toLowerCase().includes(this.reactant1.toLowerCase())
      )
        return false;
      if (
        this.reactant2 &&
        !experiment.reactant2.toLowerCase().includes(this.reactant2.toLowerCase())
      )
        return false;

      if (this.metadata) {
        const md = this.metadata;

        if (md.dateDay && experiment.dateDay !== md.dateDay) return false;
        if (md.dateMonth && experiment.dateMonth !== md.dateMonth) return false;
        if (md.dateYear && experiment.dateYear !== md.dateYear) return false;
        if (md.concentrationA && experiment.concentrationA !== md.concentrationA) return false;
        if (md.concentrationB && experiment.concentrationB !== md.concentrationB) return false;
        if (md.wavelength && experiment.wavelength !== md.wavelength) return false;

        if (
          md.scientist &&
          !experiment.scientist.toLowerCase().includes(md.scientist.toLowerCase())
        )
          return false;
        if (md.solvent && !experiment.solvent.toLowerCase().includes(md.solvent.toLowerCase()))
          return false;
        if (
          md.instrument &&
          !experiment.instrument.toLowerCase().includes(md.instrument.toLowerCase())
        )
          return false;
        if (md.modality && !experiment.modality.toLowerCase().includes(md.modality.toLowerCase()))
          return false;
        if (md.comment && !experiment.comment.toLowerCase().includes(md.comment.toLowerCase()))
          return false;

        if (md.tempFrom && experiment.temp < parseFloat(md.tempFrom)) return false;
        if (md.tempTo && experiment.temp > parseFloat(md.tempTo)) return false;

      }

      return true;
    });
  }
}
