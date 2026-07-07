import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Component,
  Output,
  EventEmitter,
  Input,
  OnChanges,
  SimpleChanges,
  OnInit,
} from '@angular/core';
import { SearchParametersDto, MeasurementSearchDto } from '../../search.dto';
import { UserService } from '../../../../shared/services/user.service';
import { NotificationService } from '../../../../core/layout/notification/notification.service';
import { GetUserDto } from '../../../../shared/dtos/user.dto';
import { TranslocoService } from '@jsverse/transloco';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';

@Component({
  selector: 'app-advanced-search',
  templateUrl: './advanced-search.component.html',
  styleUrls: ['./advanced-search.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, TRANSLOCO_IMPORTS],
})
export class AdvancedSearchComponent implements OnChanges, OnInit {
  @Output() searchChanged = new EventEmitter<SearchParametersDto>();
  @Input() searchScientist?: GetUserDto;
  @Input() set triggerClear(value: number) {
    if (value) {
      this.clearAll();
    }
  }

  allUsers: GetUserDto[] = [];
  scientistSearchText: string = '';

  params: Partial<SearchParametersDto> & {
    pFO_rateConstantMin?: number;
    pFO_rateConstantMax?: number;
    rateConstantMin?: number;
    rateConstantMax?: number;
  } = {};

  constructor(
    private userService: UserService,
    private notification: NotificationService,
    private transloco: TranslocoService,
  ) {}

  ngOnInit() {
    this.userService.getAllUsers().subscribe({
      next: (users) => (this.allUsers = users),
      error: (err) =>
        this.notification.error(
          this.transloco.translate('search.advanced.warnings.fetchUsersError'),
          err.message || err.error || err,
        ),
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['searchScientist'] && this.searchScientist) {
      this.scientistSearchText = `${this.searchScientist.firstName} ${this.searchScientist.lastName}`;
      this.params.scientist = [this.searchScientist.id];
      this.emitSearch();
    }
  }

  onScientistTyping() {
    this.searchScientist = undefined;

    if (this.scientistSearchText) {
      const lowerSearch = this.scientistSearchText.toLowerCase();
      this.params.scientist = this.allUsers
        .filter((u) => `${u.firstName} ${u.lastName}`.toLowerCase().includes(lowerSearch))
        .map((u) => u.id);
    } else {
      this.params.scientist = [];
    }
    this.emitSearch();
  }

  emitSearch() {
    const searchDto: SearchParametersDto = {
      reactantA: [],
      reactantB: [],
      dateMin: this.params.dateMin || undefined,
      dateMax: this.params.dateMax || undefined,
      scientist: this.params.scientist || [],
      temperature_Min: this.params.temperature_Min,
      temperature_Max: this.params.temperature_Max,
      solvent: this.params.solvent,
      concentrationA_Min: this.params.concentrationA_Min,
      concentrationA_Max: this.params.concentrationA_Max,
      concentrationB_Min: this.params.concentrationB_Min,
      concentrationB_Max: this.params.concentrationB_Max,
      instrument: this.params.instrument,
      modality: this.params.modality,
      wavelength_Min: this.params.wavelength_Min,
      wavelength_Max: this.params.wavelength_Max,
      comment: this.params.comment,
      measurementSearch: undefined,
    };

    if (this.params.rateConstantMin != null || this.params.rateConstantMax != null) {
      searchDto.measurementSearch = {
        rateConstantMin: this.params.rateConstantMin,
        rateConstantMax: this.params.rateConstantMax,
        pFO_rateConstantMin: this.params.pFO_rateConstantMin,
        pFO_rateConstantMax: this.params.pFO_rateConstantMax,
      };
    }

    this.searchChanged.emit(searchDto);
  }

  clearScientist() {
    this.scientistSearchText = '';
    this.searchScientist = undefined;
    this.params.scientist = [];
    this.emitSearch();
  }

  clearField(field: keyof typeof this.params) {
    this.params[field] = undefined as any;
    this.emitSearch();
  }

  clearAll() {
    this.params = {};
    this.scientistSearchText = '';
    this.searchScientist = undefined;
    this.emitSearch();
  }
}
