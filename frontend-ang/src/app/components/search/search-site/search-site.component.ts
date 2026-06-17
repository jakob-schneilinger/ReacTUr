import { Component, OnInit } from '@angular/core';
import { AdvancedSearchDto } from '../../../dtos/metadata.dto';
import { ExperimentDto } from '../../../dtos/experiment.dto';
import { ExperimentService } from '../../../services/experiment.service';
import { SearchParametersDto } from '../../../dtos/search.dto';

declare var initRDKitModule: any;

@Component({
  selector: 'app-search-site',
  templateUrl: './search-site.component.html',
  styleUrls: ['./search-site.component.scss'],
  standalone: false,
})
export class SearchSiteComponent implements OnInit {
  currentSearchState: 'initial' | 'no-results' | 'has-results' = 'initial'; //TODO: Potential für ein Ladespinner dann status loading hinzufügen
  foundExperiments: any[] = [];

  rdkitModule: any;
  sketcher1: any;
  sketcher2: any;

  clearSignal: number = 0;

  reactant1: string[] = [];
  reactant2: string[] = [];
  metadata: SearchParametersDto = { reactantA: [], reactantB: [], scientist: [] };

  isAdvancedSearchOpen: boolean = false;
  isExportModalOpen: boolean = false;
  experimentsToExport: ExperimentDto[] = [];

  // Service injecten!
  constructor(private experimentService: ExperimentService) {}

  async ngOnInit() {
    this.rdkitModule = await initRDKitModule();
  }

  toggleAdvancedSearch() {
    this.isAdvancedSearchOpen = !this.isAdvancedSearchOpen;
  }

  onClearClicked() {
    this.clearSignal = Date.now();
    this.reactant1 = [];
    this.reactant2 = [];
    this.metadata = { reactantA: [], reactantB: [], scientist: [] };

    if (this.rdkitModule && this.sketcher1 && this.sketcher2) {
      const mol = this.rdkitModule.get_mol('');
      const molBlock = mol.get_molblock();
      this.sketcher1.loadMolecule(molBlock);
      this.sketcher2.loadMolecule(molBlock);
    }
    this.executeSearch();
  }

  handleSearch(terms: { reactant1: string[]; reactant2: string[] }) {
    this.reactant1 = terms.reactant1;
    this.reactant2 = terms.reactant2;
    this.executeSearch();
  }

  handleAdvancedSearch(terms: SearchParametersDto) {
    this.metadata = terms;
    this.executeSearch();
  }

  handleSearchButton() {
    this.executeSearch();
  }

  private executeSearch() {
    const isBasicEmpty = this.reactant1.length === 0 && this.reactant2.length === 0;
    const isAdvancedEmpty =
      !this.metadata ||
      Object.values(this.metadata).every((val) => val === undefined || val === null || val === '');

    if (isBasicEmpty && isAdvancedEmpty) {
      this.currentSearchState = 'initial';
      this.foundExperiments = [];
      return;
    }

    // Zeige Ladezustand an, bis das Service antwortet
    // this.currentSearchState = 'loading';

    // Service Call anstatt lokaler Logik
    this.experimentService.searchExperiments(this.metadata).subscribe({
      next: (results) => {
        if (results.length === 0) {
          this.currentSearchState = 'no-results';
          this.foundExperiments = [];
        } else {
          this.currentSearchState = 'has-results';
          this.foundExperiments = results;
        }
      },
      error: (err) => {
        console.error('Search failed', err);
        this.currentSearchState = 'no-results';
      },
    });
  }

  handleExport(selectedIds: number[]) {
    this.experimentsToExport = this.foundExperiments.filter((exp) => selectedIds.includes(exp.id));
    this.isExportModalOpen = true;
  }
}
