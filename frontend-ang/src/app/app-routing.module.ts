import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AuthGuard } from "./guards/auth.guard";

import { HomeComponent } from "./components/home/home.component";

import { LoginComponent } from "./components/user/login/login.component";
import { ExperimentSiteComponent } from './components/experiment-site/experiment-site.component';
import { SearchSiteComponent } from './components/search-site/search-site.component';
import { ExperimentDetailsSiteComponent } from './components/experiment-details/experiment-details-site/experiment-details-site.component';


export const routes: Routes = [
  { path: 'search', component: SearchSiteComponent, canActivate: [AuthGuard] },
  { path: 'login', component: LoginComponent },
  { path: 'experiments', component: ExperimentSiteComponent, canActivate: [AuthGuard] },
  { path: 'experiments/:id', component: ExperimentDetailsSiteComponent, canActivate: [AuthGuard] },
  { path: 'reactants', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'permissions', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'admin', component: HomeComponent, canActivate: [AuthGuard] },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
