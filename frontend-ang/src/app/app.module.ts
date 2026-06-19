import { BrowserModule } from "@angular/platform-browser";
import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { HttpClientModule } from "@angular/common/http";

import { AppRoutingModule } from "./app-routing.module";
import { AppComponent } from "./app.component";
import { HeaderComponent } from "./components/shared/layout/header/header.component";
import { HomeComponent } from "./components/home/home.component";
import { LoginComponent as UserLoginComponent } from "./components/user/login/login.component";
import { UserComponent } from "./components/user/user.component";
import { NotificationsComponent } from "./components/shared/notification/notifications.component";
import { LayoutComponent } from './components/shared/layout/layout.component';
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
import { DrawMolComponent } from './components/shared/draw-mol/draw-mol.component';
import { ReactantSelectorComponent } from './components/shared/reactant-selector/reactant-selector.component';
import { SubstructureSearchComponent } from './components/search/sub-structure-search/sub-structure-search.component';
import { CdkDrag, CdkDropList } from '@angular/cdk/drag-drop';
import { ReactantSiteComponent } from './components/reactant/reactant-site/reactant-site.component';
import {
  ReactantDetailsSiteComponent
} from './components/reactant/reactant-details-site/reactant-details-site.component';
import { ReactantViewModalComponent } from './components/reactant/reactant-view-modal/reactant-view-modal.component';
import { ReactantCardComponent } from './components/reactant/reactant-card/reactant-card.component';
import {
  ReactantDetailsFormComponent
} from './components/reactant/reactant-details-site/reactant-details-form/reactant-details-form.component';
import { FileUploadComponent } from './components/shared/file-upload/file-upload.component';
import { UploadedFileComponent } from './components/shared/file-upload/uploaded-file/uploaded-file.component';
import { DragAndDropComponent } from './components/shared/file-upload/drag-and-drop/drag-and-drop.component';
import { PasteTableComponent } from './components/shared/file-upload/paste-table/paste-table.component';
import { DragAndDropDirective } from './directives/drag-and-drop.directive';
import { HasRoleDirective } from './directives/has-role.directive';
import { ExperimentViewModalComponent } from './components/experiment/experiment-view-modal/experiment-view-modal.component';
import { AdminSiteComponent } from './components/admin/admin-site/admin-site.component';
import { UserManagementComponent } from './components/admin/user-management/user-management.component';
import { OptionManagementComponent } from './components/admin/option-management/option-management.component';
import { AppModalComponent } from './components/shared/modal/app-modal.component';
import { AppConfirmModalComponent } from './components/shared/modal/confirm-modal.component';
import { ReactantStructureComponent } from './components/reactant/reactant-structure/reactant-structure.component';

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
    ExperimentViewModalComponent,
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
    ReactantCardComponent,
    ReactantDetailsSiteComponent,
    ReactantDetailsFormComponent,
    FileUploadComponent,
    UploadedFileComponent,
    DragAndDropComponent,
    PasteTableComponent,
    NotificationsComponent,
    LayoutComponent,
    AdminSiteComponent,
    UserManagementComponent,
    OptionManagementComponent,
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
    HasRoleDirective,
    AppModalComponent,
    AppConfirmModalComponent,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
