import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { HttpClient } from "@angular/common/http";
import { tap, map } from "rxjs/operators";
import { jwtDecode, JwtPayload } from 'jwt-decode';
import { Globals } from "../global/globals";
import { User } from "../dtos/user";

interface CustomJwtPayload extends JwtPayload {
  firstName: string;
  lastName: string;
  email: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private authBaseUri: string;
  private userBaseUri: string;

  constructor(
    private httpClient: HttpClient,
    private globals: Globals,
  ) {
    this.authBaseUri = globals.backendUri + '/authentication';
    this.userBaseUri = globals.backendUri + '/user';
  }

  /**
   * Login in the user. If it was successful, a valid JWT token will be stored
   *
   * @param loginDto User data

  loginUser(loginDto: UserLoginDto): Observable<string> {
    return this.httpClient
      .post<{ token: string }>(this.authBaseUri, loginDto)
      .pipe(map((res) => res.token))
      .pipe(tap((token) => this.setToken(token)));
  }
   */

  /**
   * Creates an user.
   *
   * @param createDto User data
  createUser(createDto: UserCreateDto): Observable<any> {
    return this.httpClient
      .post<{ token: string }>(this.userBaseUri + '/register', createDto)
      .pipe(map((res) => res.token))
      .pipe(tap((token) => this.setToken(token)));
  }
   */

  /**
   * Check if a valid JWT token is saved in the localStorage
   */
  isLoggedIn() {
    const token = this.getToken();

    if (token == null) {
      return false;
    }

    return this.getTokenExpirationDate(token).valueOf() > new Date().valueOf();
  }

  logoutUser() {
    console.log('Logout');
    localStorage.removeItem('authToken');
  }

  getToken() {
    return localStorage.getItem('authToken');
  }

  getLoggedInUser(): User | null {
    const encodedToken = this.getToken();
    if (encodedToken) {
      const token: CustomJwtPayload = jwtDecode(encodedToken);
      return {
        firstName: token['firstName'],
        lastName: token['lastName'],
        email: token['email'],
      };
    }
    return null;
  }

  /**
   * Returns the user role based on the current token
   */
  getUserRole() {
    const token: string | null = this.getToken();
    if (token != null) {
      const decoded: any = jwtDecode(token);
      const authInfo: string[] = decoded.rol;
      if (authInfo.includes('ROLE_ADMIN')) {
        return 'ADMIN';
      } else if (authInfo.includes('ROLE_USER')) {
        return 'USER';
      }
    }
    return 'UNDEFINED';
  }

  private setToken(authResponse: string) {
    localStorage.setItem('authToken', authResponse);
  }

  private getTokenExpirationDate(token: string): Date {
    const decoded: any = jwtDecode(token);
    if (decoded.exp === undefined) {
      return new Date(0);
    }

    const date = new Date(0);
    date.setUTCSeconds(decoded.exp);
    return date;
  }
}
