import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AuthGuard } from "./guards/auth.guard";

import { HomeComponent } from "./components/home/home.component";

import { LoginComponent } from "./components/user/login/login.component";
import { UserComponent } from "./components/user/user.component";


export const routes: Routes = [
  { path: 'search', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'login', component: LoginComponent },
  { path: 'experiments', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'reactants', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'permissions', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'admin', component: HomeComponent, canActivate: [AuthGuard] },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
