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
  isSyncing: boolean = false;

  constructor(
    private ngZone: NgZone,
    private rdkitService: RDKitService,
  ) {}

  async ngOnInit() {
    this.smilesString = this.initialSmiles;

    await this.rdkitService.initRDKit();
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.initChemDoodle();
      if (this.smilesString) {
        this.syncCanvasFromSmiles(this.smilesString);
      }
    }, 100);
  }

  private initChemDoodle() {
    this.sketcher = new ChemDoodle.SketcherCanvas(this.canvasId, 500, 300, { useServices: false });

    this.resizeSketcher();

    const canvasElement = document.getElementById(this.canvasId);
    if (canvasElement) {
      canvasElement.addEventListener('mouseup', () => this.syncCanvasToSmiles());
      canvasElement.addEventListener('touchend', () => this.syncCanvasToSmiles());
    }
  }

  onSmileInput(newValue: string) {
    this.smilesString = newValue;
    this.syncCanvasFromSmiles(newValue);
  }

  private syncCanvasFromSmiles(smiles: string) {
    if (!this.rdkitService.isLoaded || !this.sketcher) return;
    this.isSyncing = true;

    const molBlock = this.rdkitService.getMolBlockFromSmiles(smiles)
    this.sketcher.loadMolecule(ChemDoodle.readMOL(molBlock));

    setTimeout(() => (this.isSyncing = false), 50);
  }

  private syncCanvasToSmiles() {
    if (!this.sketcher || this.isSyncing) return;
    this.isSyncing = true;

    const molBlock = ChemDoodle.writeMOL(this.sketcher.getMolecule());
    const newSmiles = this.rdkitService.getSmilesFromMolBlock(molBlock);

    this.ngZone.run(() => {
      this.smilesString = newSmiles || '';
    });

    setTimeout(() => (this.isSyncing = false), 50);
  }

  onSave() {
    this.save.emit(this.smilesString);
  }

  onClose() {
    this.closeModal.emit();
  }

  onCanvasInteraction() {
    setTimeout(() => {
      this.syncCanvasToSmiles();
    }, 100);
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
