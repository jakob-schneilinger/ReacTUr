import {inject, TestBed} from '@angular/core/testing';

import {RoleGuard} from './role.guard';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {ReactiveFormsModule} from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('RoleGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [RouterTestingModule, ReactiveFormsModule],
    providers: [RoleGuard, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
  });

  it('should ...', inject([RoleGuard], (guard: RoleGuard) => {
    expect(guard).toBeTruthy();
  }));
});
