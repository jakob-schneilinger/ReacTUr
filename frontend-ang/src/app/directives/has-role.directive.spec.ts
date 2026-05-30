import { TestBed } from "@angular/core/testing";

import { HasRoleDirective } from "./has-role.directive";

describe('HasRoleDirective', () => {
  let service: HasRoleDirective;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(HasRoleDirective);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
