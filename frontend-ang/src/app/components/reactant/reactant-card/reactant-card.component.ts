import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactantDto } from '../../../dtos/reactant.dto';
import { HasRoleDirective } from '../../../directives/has-role.directive';
import { TRANSLOCO_IMPORTS } from '../../../global/transloco-imports';

@Component({
  selector: 'app-reactant-card',
  templateUrl: './reactant-card.component.html',
  styleUrls: ['./reactant-card.component.scss'],
  standalone: true,
  imports: [CommonModule, HasRoleDirective, TRANSLOCO_IMPORTS],
})
export class ReactantCardComponent {
  @Input({ required: true }) reactant!: ReactantDto;
  @Input() hasWritePermission: boolean = false;
  @Input() isArchived: boolean = false;

  @Output() view = new EventEmitter<string | undefined>();
  @Output() edit = new EventEmitter<string | undefined>();
  @Output() archive = new EventEmitter<ReactantDto>();
  @Output() restore = new EventEmitter<ReactantDto>();
  @Output() copySmiles = new EventEmitter<string>();
}
