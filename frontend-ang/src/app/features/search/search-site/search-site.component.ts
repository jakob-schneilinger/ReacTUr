import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LayoutComponent } from '../../../core/layout/layout.component';
import { ExperimentSearchComponent } from '../experiment-search/experiment-search.component';
import { AdvancedSearchComponent } from '../search-details/advanced-search.component';
import { SearchResultsComponent } from '../search-results/search-results.component';
import { ExportModalComponent } from '../export-modal/export-modal.component';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { GetExperimentDto } from '../../experiments/experiment.dto';
import { ExperimentService } from '../../experiments/experiment.service';
import { SearchParametersDto } from '../../../shared/dtos/search.dto';
import { RDKitService } from '../../../shared/services/rdkit.service';
import { GetUserDto } from '../../../shared/dtos/user.dto';

@Component({
  selector: 'app-search-site',
  templateUrl: './search-site.component.html',
  styleUrls: ['./search-site.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LayoutComponent,
    ExperimentSearchComponent,
    AdvancedSearchComponent,
    SearchResultsComponent,
    ExportModalComponent,
  ],
})
export class SearchSiteComponent implements OnInit {
  currentSearchState: 'initial' | 'no-results' | 'has-results' = 'initial'; //TODO: Potential für ein Ladespinner dann status loading hinzufügen
  foundExperiments: GetExperimentDto[] = [];

  sketcher1: any;
  sketcher2: any;

  clearSignal: number = 0;

  reactantA: string[] = [];
  reactantB: string[] = [];
  metadata: SearchParametersDto = { reactantA: [], reactantB: [], scientist: [] };

  isAdvancedSearchOpen: boolean = false;
  isExportModalOpen: boolean = false;
  experimentsToExport: GetExperimentDto[] = [];

  searchScientist?: GetUserDto;

  // Service injecten!
  constructor(
    private experimentService: ExperimentService,
    private rdkitService: RDKitService,
    private cdr: ChangeDetectorRef,
  ) {}

  async ngOnInit() {
    await this.rdkitService.initRDKit();
  }

  toggleAdvancedSearch() {
    this.isAdvancedSearchOpen = !this.isAdvancedSearchOpen;
  }

  handleSearch(terms: { reactantA: string[]; reactantB: string[] }) {
    this.reactantA = terms.reactantA;
    this.reactantB = terms.reactantB;
    this.metadata.reactantA = terms.reactantA;
    this.metadata.reactantB = terms.reactantB;
    this.executeSearch();
  }

  handleAdvancedSearch(terms: SearchParametersDto) {
    this.metadata = terms;
    // this.executeSearch();
  }

  handleSearchButton() {
    this.executeSearch();
  }

  private executeSearch() {
    const isBasicEmpty = this.reactantA.length === 0 && this.reactantB.length === 0;
    const isAdvancedEmpty =
      !this.metadata ||
      Object.values(this.metadata).every(
        (val) =>
          val === undefined ||
          val === null ||
          val === '' ||
          (Array.isArray(val) && val.length === 0),
      );

    if (isBasicEmpty && isAdvancedEmpty) {
      this.currentSearchState = 'initial';
      this.foundExperiments = [];
      return;
    }

    // Zeige Ladezustand an, bis das Service antwortet
    // this.currentSearchState = 'loading';
    this.experimentService.searchExperiments(this.metadata).subscribe({
      next: (results) => {
        console.log(results);
        if (results.length === 0) {
          this.currentSearchState = 'no-results';
          this.foundExperiments = [];
        } else {
          this.currentSearchState = 'has-results';
          this.foundExperiments = results;
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Search failed', err);
        this.currentSearchState = 'no-results';
      },
    });
  }

  handleExport(selectedUuids: string[]) {
    this.experimentsToExport = this.foundExperiments.filter((exp) =>
      selectedUuids.includes(exp.uuid),
    );
    this.isExportModalOpen = true;
  }

  handleSearchScientist(scientist: GetUserDto) {
    this.isAdvancedSearchOpen = true;
    console.log("Test1", scientist)
    this.searchScientist = scientist;
  }
}
