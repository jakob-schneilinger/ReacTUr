import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AuthGuard } from "./guards/auth.guard";

import { HomeComponent } from "./components/home/home.component";

import { LoginComponent } from "./components/user/login/login.component";
import { ExperimentSiteComponent } from './components/experiment-site/experiment-site.component';
import { SearchSiteComponent } from './components/search-site/search-site.component';
import { ExperimentDetailsSiteComponent } from './components/experiment-details-site/experiment-details-site.component';
import { ReactantSiteComponent } from './components/reactant/reactant-site/reactant-site.component';
import {
  ReactantDetailsSiteComponent
} from './components/reactant/reactant-details-site/reactant-details-site.component';


export const routes: Routes = [
  { path: 'search', component: SearchSiteComponent, canActivate: [AuthGuard] },
  { path: 'login', component: LoginComponent },
  { path: 'experiments/create', component: ExperimentDetailsSiteComponent, canActivate: [AuthGuard], data: { state: "create" } },
  { path: 'experiments/:id', component: ExperimentDetailsSiteComponent, canActivate: [AuthGuard], data: { state: "edit" } },
  { path: 'reactants', component: ReactantSiteComponent, canActivate: [AuthGuard] },
  { path: 'reactants/:name', component: ReactantDetailsSiteComponent, canActivate: [AuthGuard] },
  { path: 'permissions', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'admin', component: HomeComponent, canActivate: [AuthGuard] },
  { path: '', redirectTo: 'search', pathMatch: 'full' },
  { path: '**', redirectTo: 'search' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
