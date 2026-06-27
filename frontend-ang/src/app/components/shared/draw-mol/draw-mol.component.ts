import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  NgZone,
  HostListener,
  ChangeDetectorRef,
} from '@angular/core';
import { AppModalComponent } from '../modal/app-modal.component';
import { RDKitService } from '../../../services/rdkit.service';

declare var ChemDoodle: any;

@Component({
  selector: 'app-draw-mol',
  templateUrl: './draw-mol.component.html',
  styleUrls: ['./draw-mol.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, AppModalComponent],
})
export class DrawMolComponent implements OnInit, AfterViewInit {
  @Input() initialSmiles: string = '';

  @Output() save = new EventEmitter<string>();
  @Output() closeModal = new EventEmitter<void>();

  smilesString: string = '';
  canvasId: string = 'sketcher-modal-' + Math.random().toString(36).substring(2, 9);

  sketcher: any;

  private isUpdatingFromCanvas: boolean = false;
  private isUpdatingFromInput: boolean = false;

  private syncTimeout: any;

  constructor(
    private ngZone: NgZone,
    private rdkitService: RDKitService,
    private cdr: ChangeDetectorRef,
  ) {}

  async ngOnInit() {
    this.smilesString = this.initialSmiles;

    await this.rdkitService.initRDKit();
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.initChemDoodle();
      this.syncCanvasFromSmiles(this.smilesString || '');
    }, 100);
  }

  private initChemDoodle() {
    this.sketcher = new ChemDoodle.SketcherCanvas(this.canvasId, 500, 300, { useServices: false });

    this.resizeSketcher();

    const canvasElement = document.getElementById(this.canvasId);
    if (canvasElement) {
      canvasElement.addEventListener('mouseup', () => this.onCanvasInteraction());
      canvasElement.addEventListener('touchend', () => this.onCanvasInteraction());
    }
  }

  onSmileInput(newValue: string) {
    if (this.isUpdatingFromCanvas) return;

    this.smilesString = newValue;
    this.syncCanvasFromSmiles(newValue);
  }

  private syncCanvasFromSmiles(smiles: string) {
    if (!this.rdkitService.isLoaded || !this.sketcher) return;

    this.isUpdatingFromInput = true;

    if (!smiles || smiles.trim() === '') {
      if (typeof this.sketcher.clear === 'function') {
        this.sketcher.clear();
      }
    } else {
      const molBlock = this.rdkitService.getMolBlockFromSmiles(smiles);
      if (molBlock) {
        try {
          const chemMolecule = ChemDoodle.readMOL(molBlock);
          if (chemMolecule) {
            this.sketcher.loadMolecule(chemMolecule);
          }
        } catch (error) {
          console.warn('Failed to load molecule into ChemDoodle', error);
        }
      }
    }

    setTimeout(() => (this.isUpdatingFromInput = false), 50);
  }

  private syncCanvasToSmiles() {
    if (!this.sketcher || this.isUpdatingFromInput) return;

    this.isUpdatingFromCanvas = true;

    try {
      const molecule = this.sketcher.getMolecule();

      if (!molecule || !molecule.atoms || molecule.atoms.length === 0) {
        this.ngZone.run(() => {
          this.smilesString = '';
          this.cdr.markForCheck();
        });
      } else {
        const molBlock = ChemDoodle.writeMOL(molecule);
        if (molBlock) {
          const newSmiles = this.rdkitService.getSmilesFromMolBlock(molBlock);
          this.ngZone.run(() => {
            this.smilesString = newSmiles || '';
            this.cdr.markForCheck();
          });
        }
      }
    } catch (error) {
      console.warn('Could not read molecule from canvas:', error);
    }

    setTimeout(() => (this.isUpdatingFromCanvas = false), 50);
  }

  onCanvasInteraction() {
    if (this.syncTimeout) {
      clearTimeout(this.syncTimeout);
    }

    this.syncTimeout = setTimeout(() => {
      this.syncCanvasToSmiles();
    }, 100);
  }

  onSave() {
    this.save.emit(this.smilesString);
  }

  onClose() {
    this.closeModal.emit();
  }

  @HostListener('window:resize')
  onWindowResize() {
    this.resizeSketcher();
  }

  private resizeSketcher() {
    if (!this.sketcher) return;

    const container = document.getElementById(this.canvasId)?.parentElement;

    if (container) {
      const computedStyle = getComputedStyle(container);
      const paddingX =
        parseFloat(computedStyle.paddingLeft) + parseFloat(computedStyle.paddingRight);

      const newWidth = container.clientWidth - paddingX;

      if (newWidth > 0) {
        this.sketcher.resize(newWidth, 300);
      }
    }
  }
}
