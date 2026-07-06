
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';

export type ConcUnit = 'M' | 'mM' | 'µM';

@Component({
  selector: 'app-planning-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule, TRANSLOCO_IMPORTS],
  templateUrl: './planning-sidebar.component.html',
  styleUrls: ['./planning-sidebar.component.scss']
})
export class PlanningSidebarComponent {

  concA: number = 10;
  concB: number = 20;
  concUnit: ConcUnit = 'µM';

  durationHours: number = 1;
  durationMinutes: number = 0;

  @Input() kValue: number = 75;
  @Input() nameA: string = 'A';
  @Input() nameB: string = 'B';
  @Output() simulate = new EventEmitter<{k: number, a0: number, b0: number, duration: number, unit: ConcUnit}>();

  onSimulateClick() {
    const factor = this.concUnit === 'M' ? 1 : this.concUnit === 'mM' ? 1e-3 : 1e-6;

    const totalSeconds = (this.durationHours * 3600) + (this.durationMinutes * 60);

    this.simulate.emit({
      k: this.kValue,
      a0: this.concA * factor,
      b0: this.concB * factor,
      duration: totalSeconds,
      unit: this.concUnit
    });
  }

  onUnitChange(newUnit: ConcUnit) {
    const oldUnit = this.concUnit;

    if (oldUnit === newUnit) return;

    const factors = {
      'M': 1,
      'mM': 1000,
      'µM': 1000000
    };

    const oldFactor = factors[oldUnit];
    const newFactor = factors[newUnit];

    const multiplier = newFactor / oldFactor;

    this.concA = parseFloat((this.concA * multiplier).toPrecision(10));
    this.concB = parseFloat((this.concB * multiplier).toPrecision(10));

    this.concUnit = newUnit;
  }

  checkEmptyDuration() {
    if (this.durationHours === null || this.durationHours === undefined || this.durationHours < 0) {
      this.durationHours = 0;
    }
    if (this.durationMinutes === null || this.durationMinutes === undefined || this.durationMinutes < 0) {
      this.durationMinutes = 0;
    }
    else if (this.durationMinutes > 59) {
      this.durationMinutes = 59;
    }
  }
}
