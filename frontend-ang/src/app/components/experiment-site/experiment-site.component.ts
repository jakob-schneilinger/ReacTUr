
import { Component } from "@angular/core";

@Component({
  selector: 'app-experiment-site',
  templateUrl: './experiment-site.component.html',
  styleUrls: ['./experiment-site.component.scss'],
  standalone: false,
})
export class ExperimentSiteComponent {
  currentSearchState: 'initial' | 'no-results' | 'has-results' = 'initial';
  foundExperiments: any[] = [];

  handleSearch(terms: { reactant1: string; reactant2: string }) {
    if (!terms.reactant1 && !terms.reactant2) {
      this.currentSearchState = 'initial';
      this.foundExperiments = [];
      return;
    }

    const mockResults = this.performMockSearch(terms.reactant1, terms.reactant2);

    if (mockResults.length === 0) {
      this.currentSearchState = 'no-results';
      this.foundExperiments = [];
    } else {
      this.currentSearchState = 'has-results';
      this.foundExperiments = mockResults;
    }
  }

  handleCreate() {
    //TODO: remove when debugging done
    console.log('Navigating to create new experiment/reaction...');
    // z.B. this.router.navigate(['/create']);
  }

  private performMockSearch(r1: string, r2: string) {
    if (r1.toLowerCase() === 'x' || r2.toLowerCase() === 'x') return [];
    return [
      { id: 1, reactant1: r1, reactant2: r2, date: '2024-05-01', solvent: 'Water' },
      { id: 2, reactant1: r1, reactant2: r2, date: '2024-05-02', solvent: 'Ethanol' },
      { id: 3, reactant1: r1, reactant2: r2, date: '2024-05-03', solvent: 'Water' },
      { id: 4, reactant1: r1, reactant2: r2, date: '2024-05-04', solvent: 'Ethanol' },
      { id: 5, reactant1: r1, reactant2: r2, date: '2024-05-03', solvent: 'Water' },
      { id: 6, reactant1: r1, reactant2: r2, date: '2024-05-04', solvent: 'Ethanol' },
    ];
  }
}
