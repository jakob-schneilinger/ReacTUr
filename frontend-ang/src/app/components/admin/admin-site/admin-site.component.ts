import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../shared/layout/layout.component';
import { UserManagementComponent } from '../user-management/user-management.component';
import { OptionManagementComponent } from '../option-management/option-management.component';
import { Component } from '@angular/core';
import { TRANSLOCO_IMPORTS } from '../../../global/transloco-imports';



@Component({
  selector: 'app-admin-site',
  templateUrl: './admin-site.component.html',
  styleUrls: ['./admin-site.component.scss'],
  standalone: true,
  imports: [CommonModule, LayoutComponent, UserManagementComponent, OptionManagementComponent, TRANSLOCO_IMPORTS],
})
export class AdminSiteComponent {

}
