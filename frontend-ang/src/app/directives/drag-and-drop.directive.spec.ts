import { TestBed } from "@angular/core/testing";

import { DragAndDropDirective } from "./drag-and-drop.directive";

describe('DragAndDropDirective', () => {
  let service: DragAndDropDirective;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DragAndDropDirective);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
