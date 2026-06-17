import { Component, Output, EventEmitter, Input } from '@angular/core';
import { AdvancedSearchDto } from '../../../dtos/metadata.dto';
import { SearchParametersDto } from '../../../dtos/search.dto';
import { UserService } from '../../../services/user.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-advanced-search',
  templateUrl: './advanced-search.component.html',
  styleUrls: ['./advanced-search.component.scss'],
  standalone: false,
})
export class AdvancedSearchComponent {
  @Output() searchChanged = new EventEmitter<SearchParametersDto>();
  @Input() reactant1: string[] = [];
  @Input() reactant2: string[] = [];
  @Input() set triggerClear(value: number) {
    if (value) {
      this.clearAll();
    }
  }

  constructor(private userService: UserService, private notifications: NotificationService) {}

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
    let scientists: string[] = [];
    if (this.scientist !== '') {
      this.userService.getAllUsers().subscribe({
        next: (users) => { scientists = users.map((u) => u.uuid); },
        error: (err) => this.notifications.error("Error Fetching Users", err)
      });
    }

    // TODO: overthink this, solvent,instrument,modality want uuid
    this.searchChanged.emit({
      reactantA: [],
      reactantB: [],
      dateMin: '',
      dateMax: '',
      scientist: scientists,
      temperature_Min: parseFloat(this.tempFrom),
      temperature_Max: parseFloat(this.tempTo),
      solvent: this.solvent,
      concentrationA_Min: parseFloat('' /*this.concentrationA_Min*/),
      concentrationA_Max: parseFloat('' /*this.concentrationA_Max*/),
      concentrationB_Min: parseFloat('' /*this.concentrationB_Min*/),
      concentrationB_Max: parseFloat('' /*this.concentrationB_Max*/),
      instrument: this.instrument,
      modality: this.modality,
      wavelength_Min: parseFloat('' /*this.wavelength_Min*/),
      wavelength_Max: parseFloat('' /*this.wavelength_Max*/),
      comment: this.comment,
      measurementSearch: [],
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
    console.log(event); //TODO: remove when debugging done
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
