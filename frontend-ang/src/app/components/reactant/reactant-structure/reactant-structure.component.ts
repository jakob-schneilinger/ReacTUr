import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
  NgZone,
} from '@angular/core';

declare var $3Dmol: any;

@Component({
  selector: 'app-reactant-structure',
  templateUrl: './reactant-structure.component.html',
  styleUrls: ['./reactant-structure.component.scss'],
  standalone: false,
})
export class ReactantStructureComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() structure: File | undefined;

  @ViewChild('viewerContainer', { static: true }) viewerContainer!: ElementRef;

  private viewer: any;

  constructor(private ngZone: NgZone) {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initViewer();
      this.renderModel();
    }, 50);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['structure'] && this.structure && !changes['structure'].isFirstChange()) {
      this.renderModel();
    }
  }

  ngOnDestroy(): void {
    if (this.viewer) {
      this.viewer.clear();
      this.viewer.removeAllModels();
      this.viewer = null;
    }
  }

  private initViewer(): void {
    if (typeof $3Dmol !== 'undefined') {
      this.ngZone.runOutsideAngular(() => {
        this.viewer = $3Dmol.createViewer(this.viewerContainer.nativeElement, {
          backgroundColor: 'white',
          antialias: true, // TODO: set to false if laggy
          pixelRatio: 1,
        });
      });
    } else {
      console.error('3Dmol is missing!');
    }
  }

  private async renderModel(): Promise<void> {
    if (!this.viewer || !this.structure) {
      return;
    }

    try {
      const xyzDataString = await this.structure.text();

      if (xyzDataString.trim().startsWith('<')) {
        console.error('expected XYZ data but received HTML, check file path');
        return;
      }

      this.ngZone.runOutsideAngular(() => {
        this.viewer.clear();
        this.viewer.addModel(xyzDataString, 'xyz');
        this.viewer.setStyle({}, {
          stick: { radius: 0.15 },
          sphere: { radius: 0.4 }
        },);
      })

      this.viewer.zoomTo();
      this.viewer.render();

      setTimeout(() => {
        if (this.viewer) {
          this.viewer.resize();
        }
      }, 100);
    } catch (error) {
      console.error('error rendering the XYZ file:', error);
    }
  }
}
