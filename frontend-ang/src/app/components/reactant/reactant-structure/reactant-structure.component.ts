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
import { NotificationService } from '../../../services/notification.service';

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

  spinSpeed: number = 0.5;

  constructor(
    private ngZone: NgZone,
    private notification: NotificationService,
  ) {}

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
    if (this.viewerContainer && this.viewerContainer.nativeElement) {
      const canvas = this.viewerContainer.nativeElement;
      canvas.removeEventListener('mousedown', this.onPointerDown);
      canvas.removeEventListener('touchstart', this.onPointerDown);
      canvas.removeEventListener('mouseup', this.onPointerUp);
      canvas.removeEventListener('touchend', this.onPointerUp);
      canvas.removeEventListener('mouseleave', this.onPointerUp);
    }
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
        const canvas = this.viewerContainer.nativeElement;

        canvas.addEventListener('mousedown', this.onPointerDown);
        canvas.addEventListener('touchstart', this.onPointerDown);

        canvas.addEventListener('mouseup', this.onPointerUp);
        canvas.addEventListener('touchend', this.onPointerUp);
        canvas.addEventListener('mouseleave', this.onPointerUp);
      });
    } else {
      console.error('3Dmol is missing!');
    }
  }

  private onPointerDown = () => {
    if (this.viewer) {
      this.viewer.spin(false);
    }
  };

  private onPointerUp = () => {
    if (this.viewer) {
      this.viewer.spin('y', this.spinSpeed); // Or whatever speed you prefer
    }
  };

  private async renderModel(): Promise<void> {
    if (!this.viewer || !this.structure) {
      return;
    }

    try {
      const xyzDataString = await this.structure.text();

      if (xyzDataString.trim().startsWith('<')) {
        this.notification.error(
          'File Format Error',
          'Expected XYZ data but received HTML/XML format. Check file content!',
        );
        return;
      }

      this.ngZone.runOutsideAngular(() => {
        this.viewer.clear();
        this.viewer.addModel(xyzDataString, 'xyz');

        const atoms = this.viewer.selectedAtoms({});

        if (!atoms || atoms.length === 0) {
          this.viewer.clear();
          this.ngZone.run(() => {
            this.notification.error(
              'Invalid XYZ file',
              'The uploaded XYZ file contains no valid chemical structure. Check file content!',
            );
          });
          return;
        }
        this.viewer.setStyle(
          {},
          {
            stick: { radius: 0.15 },
            sphere: { radius: 0.4 },
          },
        );

        this.viewer.zoomTo();
        this.viewer.render();

        this.viewer.spin('y', this.spinSpeed);
      });

      setTimeout(() => {
        if (this.viewer) {
          this.viewer.resize();
        }
      }, 100);
    } catch (error) {
      console.error('Error rendering the XYZ file:', error);
    }
  }
}
