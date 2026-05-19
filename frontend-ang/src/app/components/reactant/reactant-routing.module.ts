import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ReactantSiteComponent } from './reactant-site/reactant-site.component';
import { ReactantDetailsSiteComponent } from './reactant-details-site/reactant-details-site.component';

const routes: Routes = [
  { path: '', component: ReactantSiteComponent }, // /reactants
  { path: 'create', component: ReactantDetailsSiteComponent, data: { state: 'create' } }, // /reactants/create
  { path: ':name', component: ReactantDetailsSiteComponent, data: { state: 'edit' } }, // /reactants/:name
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ReactantRoutingModule {}
