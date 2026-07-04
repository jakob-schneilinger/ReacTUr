import { CommonModule } from '@angular/common';
import { LayoutComponent } from '../../../../core/layout/layout.component';
import { UserManagementComponent } from '../../components/user-managment/user-management.component';
import { OptionManagementComponent } from '../../components/option-managment/option-management.component';
import { Component } from '@angular/core';
import { TRANSLOCO_IMPORTS } from '../../../../core/global/transloco-imports';



@Component({
  selector: 'app-admin-site',
  templateUrl: './admin-page.component.html',
  styleUrls: ['./admin-page.component.scss'],
  standalone: true,
  imports: [CommonModule, LayoutComponent, UserManagementComponent, OptionManagementComponent, TRANSLOCO_IMPORTS],
})
export class AdminPageComponent {

}
