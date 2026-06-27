import { CommonModule } from '@angular/common';
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
  ChangeDetectorRef
} from '@angular/core';
import { NotificationService } from '../../../services/notification.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TRANSLOCO_IMPORTS } from '../../../global/transloco-imports';
import { TranslocoService } from '@jsverse/transloco';
import { RDKitService } from '../../../services/rdkit.service';

declare var $3Dmol: any;

@Component({
  selector: 'app-reactant-structure',
  templateUrl: './reactant-structure.component.html',
  styleUrls: ['./reactant-structure.component.scss'],
  standalone: true,
  imports: [CommonModule, TRANSLOCO_IMPORTS],
})
export class ReactantStructureComponent implements AfterViewInit, OnInit, OnChanges, OnDestroy {
  @Input() structure: File | undefined;
  @Input() smiles: string = '';
  @Input() loading: boolean = false;

  @ViewChild('viewerContainer', { static: true }) viewerContainer!: ElementRef;

  private viewer: any;
  private rdkitModule: any = null;

  spinSpeed: number = 0.5;
  viewMode: '2D' | '3D' = '2D';

  svg2D: SafeHtml | null = null;

  constructor(
    private ngZone: NgZone,
    private notification: NotificationService,
    private rdkitService: RDKitService,
    private sanitizer: DomSanitizer,
    private cd: ChangeDetectorRef,
    private transloco: TranslocoService,
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      await this.rdkitService.initRDKit();

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
      if (this.rdkitService.isLoaded) {
        this.render2DModel();
      }
      this.renderModel();
    }, 50);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['structure'] && this.structure && !changes['structure'].isFirstChange()) {
      this.renderModel();
    }
    if (changes['smiles'] && !changes['smiles'].isFirstChange()) {
      this.render2DModel();
    }
    if (changes['loading'] && !this.loading) {
      setTimeout(() => {
        if (this.viewer) {
          this.viewer.resize();
        }
      }, 100);
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
          antialias: true,
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
    if (!this.structure) return;

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
          this.transloco.translate('reactants.structure.notifications.fileFormatErrorTitle'),
          this.transloco.translate('reactants.structure.notifications.fileFormatErrorMessage'),
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
              this.transloco.translate('reactants.structure.notifications.invalidXyzTitle'),
              this.transloco.translate('reactants.structure.notifications.invalidXyzMessage'),
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
    const svg = this.rdkitService.getSvgFromSmiles(this.smiles);
    if (svg) {
      this.svg2D = this.sanitizer.bypassSecurityTrustHtml(svg);
      this.cd.markForCheck();
    } else {
      this.svg2D = null;
      if (this.smiles) {
        this.notification.error(
          this.transloco.translate('reactants.structure.notifications.render2dErrorTitle'),
          this.transloco.translate('reactants.structure.notifications.render2dErrorMessage'),
        );
      }
    }
  }
}
