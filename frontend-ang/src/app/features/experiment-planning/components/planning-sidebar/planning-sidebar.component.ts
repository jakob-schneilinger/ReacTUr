
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
  concA: number = 0.00001; // 10 µM in Molar
  concB: number = 0.00002; // 20 µM in Molar
  duration: number = 10000; // Dauer in Sekunden

  @Output() simulate = new EventEmitter<{k: number, a0: number, b0: number, duration: number}>();

  onSimulateClick() {
    this.simulate.emit({
      k: this.kValue,
      a0: this.concA,
      b0: this.concB,
      duration: this.duration
    });
  }
}
