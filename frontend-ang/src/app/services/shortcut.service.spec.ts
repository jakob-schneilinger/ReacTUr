import {TestBed} from '@angular/core/testing';

import {ShortcutService} from './shortcut.service';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {ReactiveFormsModule} from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ShortcutService', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, ReactiveFormsModule],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }),
  );

  it('should be created', () => {
    const service: ShortcutService = TestBed.inject(ShortcutService);
    expect(service).toBeTruthy();
  });
});
