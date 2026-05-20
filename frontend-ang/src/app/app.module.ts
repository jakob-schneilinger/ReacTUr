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
import { CommonModule, NgOptimizedImage } from '@angular/common';

import { NgbModule } from "@ng-bootstrap/ng-bootstrap";
import { OAuthModule } from "angular-oauth2-oidc";
import { ExperimentSearchComponent } from './components/search/experiment-search/experiment-search.component';
import { SearchResultsComponent } from './components/search/search-results/search-results.component';
import { SearchSiteComponent } from './components/search/search-site/search-site.component';
import { AdvancedSearchComponent } from './components/search/search-details/advanced-search.component';
import { ExperimentDetailsSiteComponent } from './components/experiment/experiment-details-site/experiment-details-site.component';
import { ExperimentDetailsFormComponent } from './components/experiment/experiment-details-site/experiment-detail-form/experiment-details-form.component';
import { ExportModalComponent } from './components/export/export-modal.component';
import { DrawMolComponent } from './components/draw-mol/draw-mol.component';
import { ReactantSelectorComponent } from './components/reactant-selector/reactant-selector.component';
import { SubstructureSearchComponent } from './components/search/sub-structure-search/sub-structure-search.component';
import { CdkDrag, CdkDropList } from '@angular/cdk/drag-drop';
import { ReactantSiteComponent } from './components/reactant/reactant-site/reactant-site.component';
import {
  ReactantDetailsSiteComponent
} from './components/reactant/reactant-details-site/reactant-details-site.component';
import { ReactantViewModalComponent } from './components/reactant/reactant-view-modal/reactant-view-modal.component';
import { ReactantStructureComponent } from './components/reactant/reactant-structure/reactant-structure.component';
import {
  ReactantDetailsFormComponent
} from './components/reactant/reactant-details-site/reactant-details-form/reactant-details-form.component';
import { FileUploadComponent } from './components/file-upload/file-upload.component';
import { UploadedFileComponent } from './components/file-upload/uploaded-file/uploaded-file.component';
import { DragAndDropComponent } from './components/file-upload/drag-and-drop/drag-and-drop.component';
import { PasteTableComponent } from './components/file-upload/paste-table/paste-table.component';
import { DragAndDropDirective } from './directives/drag-and-drop.directive';

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
    SearchSiteComponent,
    AdvancedSearchComponent,
    ExperimentDetailsSiteComponent,
    ExportModalComponent,
    ExperimentDetailsFormComponent,
    ReactantSiteComponent,
    ReactantDetailsSiteComponent,
    ReactantViewModalComponent,
    ReactantStructureComponent,
    ReactantDetailsFormComponent,
    FileUploadComponent,
    UploadedFileComponent,
    DragAndDropComponent,
    PasteTableComponent,
    NotificationsComponent,
    LayoutComponent,
  ],
  imports: [
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
    CommonModule,
    DragAndDropDirective,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
