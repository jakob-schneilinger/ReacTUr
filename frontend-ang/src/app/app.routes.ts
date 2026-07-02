import { Routes } from "@angular/router";
import { AuthGuard } from './core/auth/auth.guard';
import { RoleGuard } from './core/auth/role.guard';

import { LoginComponent } from "./features/login/login.component";
import { SearchSiteComponent } from './features/search/search-site/search-site.component';
import { ExperimentDetailsSiteComponent } from './features/experiments/experiment-details/experiment-details-site.component';
import { ReactantDetailsSiteComponent } from './features/reactants/reactant-details/reactant-details-site.component';
import { ReactantSiteComponent } from './features/reactants/reactant-site/reactant-site.component';
import { AdminSiteComponent } from './features/admin/admin-site/admin-site.component';

export const routes: Routes = [
  { path: 'search', component: SearchSiteComponent, canActivate: [AuthGuard] },
  { path: 'login', component: LoginComponent },
  {
    path: 'experiments/create',
    component: ExperimentDetailsSiteComponent,
    canActivate: [AuthGuard],
    data: { state: 'create'},
  },
  {
    path: 'experiments/:uuid',
    component: ExperimentDetailsSiteComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { state: 'edit', roles: ['ADMIN', 'SUPERVISOR'] },
  },
  { path: 'reactants', component: ReactantSiteComponent, canActivate: [AuthGuard] },
  {
    path: 'reactants/create',
    component: ReactantDetailsSiteComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { state: 'create', roles: ['ADMIN', 'SUPERVISOR'] },
  },
  {
    path: 'reactants/:uuid',
    component: ReactantDetailsSiteComponent,
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
