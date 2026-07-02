import { Routes } from "@angular/router";
import { AuthGuard } from './core/auth/auth.guard';
import { RoleGuard } from './core/auth/role.guard';

import { LoginComponent } from "./features/login/login.component";
import { SearchPageComponent } from './features/search/pages/search-page/search-page.component';
import { ExperimentDetailsPageComponent } from './features/experiments/pages/experiment-details-page/experiment-details-page.component';
import { ReactantDetailsPageComponent } from './features/reactants/pages/reactant-details-page/reactant-details-page.component';
import { ReactantOverviewPageComponent } from './features/reactants/pages/reactant-overview-page/reactant-overview-page.component';
import { AdminSiteComponent } from './features/admin/admin-site/admin-site.component';

export const routes: Routes = [
  { path: 'search', component: SearchPageComponent, canActivate: [AuthGuard] },
  { path: 'login', component: LoginComponent },
  {
    path: 'experiments/create',
    component: ExperimentDetailsPageComponent,
    canActivate: [AuthGuard],
    data: { state: 'create'},
  },
  {
    path: 'experiments/:uuid',
    component: ExperimentDetailsPageComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { state: 'edit', roles: ['ADMIN', 'SUPERVISOR'] },
  },
  { path: 'reactants', component: ReactantOverviewPageComponent, canActivate: [AuthGuard] },
  {
    path: 'reactants/create',
    component: ReactantDetailsPageComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { state: 'create', roles: ['ADMIN', 'SUPERVISOR'] },
  },
  {
    path: 'reactants/:uuid',
    component: ReactantDetailsPageComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { state: 'edit', roles: ['ADMIN', 'SUPERVISOR'] },
  },
  {
    path: 'admin',
    component: AdminSiteComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] },
  },
  { path: '', redirectTo: 'search', pathMatch: 'full' },
  { path: '**', redirectTo: 'search' },
];
