import { AfterViewInit, Component, EventEmitter, Input, OnInit, Output, NgZone } from '@angular/core';

declare var initRDKitModule: any;
declare var ChemDoodle: any;

@Component({
  selector: 'app-draw-mol',
  templateUrl: './draw-mol.component.html',
  styleUrls: ['./draw-mol.component.scss'],
  standalone: false,
})
export class DrawMolComponent implements OnInit, AfterViewInit {
  @Input() initialSmiles: string = '';

  @Output() save = new EventEmitter<string>();
  @Output() closeModal = new EventEmitter<void>();

  smilesString: string = '';
  canvasId: string = 'sketcher-modal-' + Math.random().toString(36).substring(2, 9);

  sketcher: any;
  rdkitModule: any;
  isSyncing: boolean = false;

  constructor(private ngZone: NgZone) {}

  async ngOnInit() {
    this.smilesString = this.initialSmiles;

    if (!(window as any)['RDKit']) {
      this.rdkitModule = await initRDKitModule();
      (window as any)['RDKit'] = this.rdkitModule;
    } else {
      this.rdkitModule = (window as any)['RDKit'];
    }
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
    if (!this.rdkitModule || !this.sketcher) return;

    this.isSyncing = true;
    try {
      if (!smiles) {
        const emptyMol = this.rdkitModule.get_mol('');
        this.sketcher.loadMolecule(ChemDoodle.readMOL(emptyMol.get_molblock()));
        emptyMol.delete();
      } else {
        const mol = this.rdkitModule.get_mol(smiles);
        if (mol) {
          const molBlock = mol.get_molblock();
          this.sketcher.loadMolecule(ChemDoodle.readMOL(molBlock));
          mol.delete();
        }
      }
    } catch (error) {
      console.warn('Invalid SMILES input', error);
    }
    setTimeout(() => this.isSyncing = false, 50);
  }

  private syncCanvasToSmiles() {
    if (!this.rdkitModule || !this.sketcher || this.isSyncing) return;

    this.isSyncing = true;
    try {
      const molBlock = ChemDoodle.writeMOL(this.sketcher.getMolecule());
      const mol = this.rdkitModule.get_mol(molBlock);

      this.ngZone.run(() => {
        this.smilesString = mol.get_smiles();
      });

      mol.delete();
    } catch (error) {
      this.ngZone.run(() => {
        this.smilesString = '';
      });
    }
    setTimeout(() => this.isSyncing = false, 50);
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
}
