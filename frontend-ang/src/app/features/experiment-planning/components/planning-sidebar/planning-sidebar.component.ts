
import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';

@Component({
  selector: 'app-planning-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule, TRANSLOCO_IMPORTS],
  templateUrl: './planning-sidebar.component.html',
  styleUrls: ['./planning-sidebar.component.scss']
})
export class PlanningSidebarComponent {

  // Diese Werte binden wir per ngModel ans HTML
  kValue: number = 75;
  concA_mM: number = 10;
  concB_mM: number = 20;
  durationHours: number = 2;

  @Output() simulate = new EventEmitter<{k: number, a0: number, b0: number, duration: number}>();

  onSimulateClick() {
    this.simulate.emit({
      k: this.kValue,
      a0: this.concA_mM / 1000,
      b0: this.concB_mM / 1000,
      duration: this.durationHours * 3600
    });
  }
}
