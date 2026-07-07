import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LayoutComponent } from '../../../../core/layout/layout.component';
import { StandardSearchComponent } from '../../components/standard-search/standard-search.component';
import { AdvancedSearchComponent } from '../../components/advanced-search/advanced-search.component';
import { SearchResultsComponent } from '../../components/search-results/search-results.component';
import { ExportModalComponent } from '../../components/export-modal/export-modal.component';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { GetExperimentDto } from '../../../experiments/experiment.dto';
import { ExperimentService } from '../../../experiments/experiment.service';
import { SearchParametersDto } from '../../search.dto';
import { RDKitService } from '../../../../shared/services/rdkit.service';
import { GetUserDto } from '../../../../shared/dtos/user.dto';
import { SearchService } from '../../search.service';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';

@Component({
  selector: 'app-search-site',
  templateUrl: './search-page.component.html',
  styleUrls: ['./search-page.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LayoutComponent,
    StandardSearchComponent,
    AdvancedSearchComponent,
    SearchResultsComponent,
    ExportModalComponent,
    TRANSLOCO_IMPORTS
  ],
})
export class SearchPageComponent implements OnInit {
  currentSearchState: 'initial' | 'no-results' | 'has-results' = 'initial';
  foundExperiments: GetExperimentDto[] = [];

  clearSignal: number = 0;

  reactantA: string[] = [];
  reactantB: string[] = [];
  metadata: SearchParametersDto = { reactantA: [], reactantB: [], scientist: [] };

  isAdvancedSearchOpen: boolean = false;
  isExportModalOpen: boolean = false;
  experimentsToExport: GetExperimentDto[] = [];

  searchScientist?: GetUserDto;

  currentPage: number = 0;
  pageSize: number = 25;
  totalElements: number = 0;

  constructor(
    private searchService: SearchService,
    private rdkitService: RDKitService,
    private cdr: ChangeDetectorRef,
  ) {}

  async ngOnInit() {
    await this.rdkitService.initRDKit();
  }

  handleSearchButton() {
    this.currentPage = 0;
    this.executeSearch();
  }

  handlePageChange(newPage: number) {
    this.currentPage = newPage;
    this.executeSearch();
  }

  toggleAdvancedSearch() {
    this.isAdvancedSearchOpen = !this.isAdvancedSearchOpen;
  }

  handleSearch(terms: { reactantA: string[]; reactantB: string[] }) {
    this.reactantA = terms.reactantA;
    this.reactantB = terms.reactantB;
    this.metadata.reactantA = terms.reactantA;
    this.metadata.reactantB = terms.reactantB;
    // this.executeSearch();
  }

  handleAdvancedSearch(terms: SearchParametersDto) {
    this.metadata = terms;
    // this.executeSearch();
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

    this.searchService.searchExperiments(this.metadata, this.currentPage, this.pageSize).subscribe({
      next: (pageResults) => {
        console.log('Search Resuluts Page: ', pageResults);
        if (pageResults.content.length === 0) {
          this.currentSearchState = 'no-results';
          this.foundExperiments = [];
          this.totalElements = 0;
        } else {
          this.currentSearchState = 'has-results';
          this.foundExperiments = pageResults.content;
          this.totalElements = pageResults.totalElements;
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Search failed', err);
        this.currentSearchState = 'no-results';
      },
    });
  }

  handleExport(selectedExperiments: GetExperimentDto[]) {
    this.experimentsToExport = selectedExperiments;
    this.isExportModalOpen = true;
  }

  handleExportAll() {
    this.searchService.searchExperiments(this.metadata, 0, this.totalElements).subscribe({
      next: (pageResults) => {
        this.experimentsToExport = pageResults.content;
        this.isExportModalOpen = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to fetch all experiments for export', err);
      },
    });
  }

  handleSearchScientist(scientist: GetUserDto) {
    this.isAdvancedSearchOpen = true;
    console.log('Test1', scientist);
    this.searchScientist = scientist;
  }

  handlePageSizeChange(newSize: number) {
    this.pageSize = newSize;
    this.currentPage = 0;
    this.executeSearch();
  }
}
