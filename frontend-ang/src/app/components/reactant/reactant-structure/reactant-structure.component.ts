import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnInit,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
  NgZone,
} from '@angular/core';
import { NotificationService } from '../../../services/notification.service';
import { timeout } from 'rxjs';

declare var $3Dmol: any;
declare var initRDKitModule: any;

@Component({
  selector: 'app-reactant-structure',
  templateUrl: './reactant-structure.component.html',
  styleUrls: ['./reactant-structure.component.scss'],
  standalone: false,
})
export class ReactantStructureComponent implements AfterViewInit, OnInit, OnChanges, OnDestroy {
  @Input() structure: File | undefined;
  @Input() smiles: string = '';

  @ViewChild('viewerContainer', { static: true }) viewerContainer!: ElementRef;
  @ViewChild('rdkit2dContainer', { static: true }) rdkit2dContainer!: ElementRef;

  private viewer: any;
  private rdkitModule: any = null;

  spinSpeed: number = 0.5;

  viewMode: '2D' | '3D' = '2D';

  constructor(
    private ngZone: NgZone,
    private notification: NotificationService,
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      if (!(window as any)['RDKit']) {
        this.rdkitModule = await initRDKitModule();
        (window as any)['RDKit'] = this.rdkitModule;
      } else {
        this.rdkitModule = (window as any)['RDKit'];
      }

      if (this.smiles) {
        this.render2DModel();
      }
    } catch (error) {
      console.error('Failed to initialize RDKit module:', error);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initViewer();
      if (this.rdkitModule) {
        this.render2DModel();
      }
      this.renderModel();
    }, 50);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['structure'] && this.structure && !changes['structure'].isFirstChange()) {
      this.renderModel();
    }
    if (changes['smiles'] && this.smiles && !changes['smiles'].isFirstChange()) {
      this.render2DModel();
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
      this.viewer.spin(false);

      try {
        this.viewer.removeAllModels();
        this.viewer.clear();
      } catch (error) {
        console.warn('3Dmol teardown warning:', error);
      } finally {
        this.viewer = null;
      }
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
      this.viewer.spin('y', this.spinSpeed);
    }
  };

  toggleView(): void {
    this.viewMode = this.viewMode === '2D' ? '3D' : '2D';

    if (this.viewer) {
      if (this.viewMode === '3D') {
        this.viewer.spin('y', this.spinSpeed);
      } else {
        this.viewer.spin(false);
      }
    }
  }

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

  private render2DModel(): void {
    if (!this.smiles || !this.rdkitModule) {
      return;
    }

    try {
      const mol = this.rdkitModule.get_mol(this.smiles);
      if (mol) {
        const svg = mol.get_svg();

        this.rdkit2dContainer.nativeElement.innerHTML = svg;

        mol.delete();
      }
    } catch (error) {
      console.log('Error rendering 2D structure with RDKit:', error);
      this.notification.error('2D Render Error', 'Could not generate 2D structure from SMILES.');
    }
  }
}
