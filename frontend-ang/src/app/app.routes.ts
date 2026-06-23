import { Routes } from "@angular/router";
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';

import { LoginComponent } from "./components/user/login/login.component";
import { SearchSiteComponent } from './components/search/search-site/search-site.component';
import { ExperimentDetailsSiteComponent } from './components/experiment/experiment-details-site/experiment-details-site.component';
import { ReactantDetailsSiteComponent } from './components/reactant/reactant-details-site/reactant-details-site.component';
import { ReactantSiteComponent } from './components/reactant/reactant-site/reactant-site.component';
import { AdminSiteComponent } from './components/admin/admin-site/admin-site.component';

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
