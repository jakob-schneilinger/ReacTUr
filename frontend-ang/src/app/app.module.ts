import { BrowserModule } from "@angular/platform-browser";
import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { HttpClientModule } from "@angular/common/http";

import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { HeaderComponent } from "./components/header/header.component";
import { HomeComponent } from "./components/home/home.component";
import { LoginComponent as UserLoginComponent } from "./components/user/login/login.component";
import { UserComponent } from "./components/user/user.component";
import { NotificationsComponent } from "./components/notification/notifications.component";
import { LayoutComponent } from './components/layout/layout.component';
import { NgOptimizedImage } from "@angular/common";

import { NgbModule } from "@ng-bootstrap/ng-bootstrap";
import { OAuthModule } from "angular-oauth2-oidc";
import { ExperimentSearchComponent } from './components/experiment-search/experiment-search.component';
import { SearchResultsComponent } from './components/search/search-results/search-results.component';
import { ExperimentSiteComponent } from './components/experiment-site/experiment-site.component';
import { SearchSiteComponent } from './components/search/search-site/search-site.component';
import { AdvancedSearchComponent } from './components/search/search-details/advanced-search.component';
import { ExperimentDetailsSiteComponent } from './components/experiment-details-site/experiment-details-site.component';
import { ExperimentCreateSiteComponent } from './components/experiment-create/experiment-create-site/experiment-create-site.component';
import { ExperimentDetailsFormComponent } from './components/experiment-details-site/experiment-detail-form/experiment-details-form.component';
import { ExportModalComponent } from './components/export/export-modal.component';
import { DrawMolComponent } from './components/draw-mol/draw-mol.component';
import { ReactantSelectorComponent } from './components/reactant-selector/reactant-selector.component';
import { SubstructureSearchComponent } from './components/search/sub-structure-search/sub-structure-search.component';
import { CdkDrag, CdkDropList } from '@angular/cdk/drag-drop';
import { SharedModule } from './shared/shared.module';

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    HomeComponent,
    UserLoginComponent,
    UserComponent,
    DrawMolComponent,
    ReactantSelectorComponent,
    ExperimentSearchComponent,
    SubstructureSearchComponent,
    SearchResultsComponent,
    ExperimentSiteComponent,
    SearchSiteComponent,
    AdvancedSearchComponent,
    ExperimentDetailsSiteComponent,
    ExportModalComponent,
    ExperimentCreateSiteComponent,
    ExperimentDetailsFormComponent,
  ],
  imports: [
    SharedModule,
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    HttpClientModule,
    NgOptimizedImage,
    NgbModule,
    OAuthModule.forRoot({
      resourceServer: {
        allowedUrls: ['http://localhost:8080/api'],
        sendAccessToken: true,
      },
    }),
    CdkDropList,
    CdkDrag,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
