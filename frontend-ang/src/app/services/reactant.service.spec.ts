import { TestBed } from '@angular/core/testing';

import { ReactantService } from './reactant.service';

describe('ReactantService', () => {
  let service: ReactantService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReactantService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
