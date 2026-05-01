import { Injectable } from "@angular/core";
import { map, Observable, of, tap } from "rxjs";
import {User} from "../dtos/user";
import { AuthService } from "./auth.service";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Globals } from "../global/globals";

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private userBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
    private authService: AuthService,
  ) {
    this.userBaseUri = this.globals.backendUri + '/user';
  }

  isCurrentLoggedIn(user: User) {
    const loggedInUser = this.authService.getLoggedInUser();
    return loggedInUser != null && loggedInUser.email == user.email;
  }
}
