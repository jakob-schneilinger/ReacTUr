import { NgModule } from '@angular/core';
import { ReactantRoutingModule } from './reactant-routing.module';

import { ReactantSiteComponent } from './reactant-site/reactant-site.component';
import { ReactantDetailsSiteComponent } from './reactant-details-site/reactant-details-site.component';
import { ReactantViewModalComponent } from './reactant-view-modal/reactant-view-modal.component';
import { ReactantStructureComponent } from './reactant-structure/reactant-structure.component';
import { SharedModule } from '../../shared/shared.module';
import { FormsModule } from '@angular/forms';

@NgModule({
  declarations: [
    ReactantSiteComponent,
    ReactantDetailsSiteComponent,
    ReactantViewModalComponent,
    ReactantStructureComponent,
  ],
  imports: [SharedModule, ReactantRoutingModule, FormsModule],
})
export class ReactantModule {}
