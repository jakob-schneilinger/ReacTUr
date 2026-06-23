import { CommonModule } from '@angular/common';
import { ReactantSelectorComponent } from '../../shared/reactant-selector/reactant-selector.component';
import { SubstructureSearchComponent } from '../sub-structure-search/sub-structure-search.component';
import { Component, Output, EventEmitter, Input, OnInit, ChangeDetectorRef } from '@angular/core';
import { ReactantDto } from '../../../dtos/reactant.dto';
import { ReactantService } from '../../../services/reactant.service';
import { ExperimentService } from '../../../services/experiment.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-experiment-search',
  templateUrl: './experiment-search.component.html',
  styleUrls: ['./experiment-search.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactantSelectorComponent, SubstructureSearchComponent],
})
export class ExperimentSearchComponent implements OnInit {
  @Output() searchChanged = new EventEmitter<{
    reactantA: string[];
    reactantB: string[];
  }>();

  triggerClearValue1: number = 0;
  triggerClearValue2: number = 0;

  @Input() set triggerClear(value: number) {
    if (value) {
      this.triggerClearValue1 = value;
      this.triggerClearValue2 = value;
      this.currentExact1 = [];
      this.currentExact2 = [];
      this.currentSub1 = [];
      this.currentSub2 = [];
      this.showSub1 = false;
      this.showSub2 = false;
    }
  }

  initVal1: string[] = [];
  initVal2: string[] = [];

  @Input() set reactantA(val: string[]) {
    if (val) this.initVal1 = val;
  }
  @Input() set reactantB(val: string[]) {
    if (val) this.initVal2 = val;
  }

  showSub1 = false;
  showSub2 = false;

  // Updated to arrays to support multi-select changes
  currentExact1: string[] = [];
  currentExact2: string[] = [];
  currentSub1: string[] = [];
  currentSub2: string[] = [];

  availableReactants: ReactantDto[] = [];

  constructor(
    private reactantService: ReactantService,
    private notification: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.reactantService.getAllReactants().subscribe({
      next: (value) => {
        this.availableReactants = value;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.notification.error(
          'Reactant Error',
          `Unable to fetch reactants. Please reload the page! Error: ${err.message || err.error || err} `,
        );
      },
    });
  }

  toggleMode(num: 1 | 2) {
    if (num === 1) {
      this.showSub1 = !this.showSub1;
      if (this.showSub1) {
        this.currentExact1 = [];
        this.triggerClearValue1++;
      } else {
        this.currentSub1 = [];
      }
    } else {
      this.showSub2 = !this.showSub2;
      if (this.showSub2) {
        this.currentExact2 = [];
        this.triggerClearValue2++;
      } else {
        this.currentSub2 = [];
      }
    }

    this.emitSearch();
  }

  onReactantChange(num: 1 | 2, values: string[]) {
    if (num === 1) {
      this.currentExact1 = values;
    } else {
      this.currentExact2 = values;
    }
    this.emitSearch();
  }

  onSubstructureMatch(num: 1 | 2, matchedNames: string[]) {
    if (num === 1) this.currentSub1 = matchedNames;
    else this.currentSub2 = matchedNames;
    this.emitSearch();
  }

  emitSearch() {
    const r1 = this.showSub1 ? this.currentSub1 : this.currentExact1;
    const r2 = this.showSub2 ? this.currentSub2 : this.currentExact2;

    this.searchChanged.emit({
      reactantA: r1,
      reactantB: r2,
    });
  }
}
