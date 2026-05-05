import { Component, Output, EventEmitter, Input } from '@angular/core';
import { AdvancedSearch } from '../../dtos/metadata';

@Component({
  selector: 'app-advanced-search',
  templateUrl: './advanced-search.component.html',
  styleUrls: ['./advanced-search.component.scss'],
  standalone: false,
})
export class AdvancedSearchComponent {
  @Output() searchChanged = new EventEmitter<AdvancedSearch>();
  @Input() reactant1: string = '';
  @Input() reactant2: string = '';
  @Input() set triggerClear(value: number) {
    if (value) {
      this.clearAll();
    }
  }

  dateDay: string = '';
  dateMonth: string = '';
  dateYear: string = '';
  scientist: string = '';
  tempFrom: string = '';
  tempTo: string = '';
  solvent: string = '';
  concentrationA: string = '';
  concentrationB: string = '';
  instrument: string = '';
  modality: string = '';
  wavelength: string = '';
  comment: string = '';
  minRange: string = '';
  maxRange: string = '';

  onSearch() {
    this.searchChanged.emit({
      dateDay: this.dateDay,
      dateMonth: this.dateMonth,
      dateYear: this.dateYear,
      scientist: this.scientist,
      tempFrom: this.tempFrom,
      tempTo: this.tempTo,
      solvent: this.solvent,
      concentrationA: this.concentrationA,
      concentrationB: this.concentrationB,
      instrument: this.instrument,
      modality: this.modality,
      wavelength: this.wavelength,
      comment: this.comment,
      minRange: this.minRange,
      maxRange: this.maxRange,
    });
  }

  // --- Calendar Functions ---
  validateDay() {
    if (this.dateDay !== null && this.dateDay !== '') {
      let val = Number(this.dateDay);
      if (val > 31) this.dateDay = '31';
      if (val < 1) this.dateDay = '1';
    }
    this.onSearch();
  }

  validateMonth() {
    if (this.dateMonth !== null && this.dateMonth !== '') {
      let val = Number(this.dateMonth);
      if (val > 12) this.dateMonth = '12';
      if (val < 1) this.dateMonth = '1';
    }
    this.onSearch();
  }

  validateYear() {
    if (this.dateYear !== null && this.dateYear !== '') {
      let val = Number(this.dateYear);
      if (val > 2100) this.dateYear = '2500';
    }
    this.onSearch();
  }

  onCalendarSelect(event: any) {
    console.log(event)
    const selectedDate = event.target.value;
    if (selectedDate) {
      const parts = selectedDate.split('-');
      this.dateYear = parts[0];
      this.dateMonth = parts[1];
      this.dateDay = parts[2];
      this.onSearch();
    } else if (selectedDate === '') {
      this.dateYear = '';
      this.dateMonth = '';
      this.dateDay = '';
      this.onSearch();
    }
  }

  // --- Individual Clear Functions ---
  clearScientist() {
    this.scientist = '';
    this.onSearch();
  }
  clearSolvent() {
    this.solvent = '';
    this.onSearch();
  }
  clearInstrument() {
    this.instrument = '';
    this.onSearch();
  }
  clearModality() {
    this.modality = '';
    this.onSearch();
  }

  // --- Clear All ---
  clearAll() {
    this.dateDay = '';
    this.dateMonth = '';
    this.dateYear = '';
    this.scientist = '';
    this.tempFrom = '';
    this.tempTo = '';
    this.solvent = '';
    this.concentrationA = '';
    this.concentrationB = '';
    this.instrument = '';
    this.modality = '';
    this.wavelength = '';
    this.comment = '';
    this.minRange = '';
    this.maxRange = '';

    this.onSearch();
  }
}
