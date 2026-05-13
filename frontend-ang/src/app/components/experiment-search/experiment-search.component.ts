import { Component, Output, EventEmitter, Input, OnInit } from '@angular/core';
import { Reactant } from '../../dtos/reactant';

@Component({
  selector: 'app-experiment-search',
  templateUrl: './experiment-search.component.html',
  styleUrls: ['./experiment-search.component.scss'],
  standalone: false,
})
export class ExperimentSearchComponent implements OnInit {
  // Emitted weiterhin Strings, um den Parent nicht zu brechen!
  @Output() searchChanged = new EventEmitter<{ reactant1: string; reactant2: string }>();

  @Input() set triggerClear(value: number) {
    if (value) {
      this.clearAll();
    }
  }

  // Für URL-Parameter initialisierung
  @Input() set reactant1(val: string) { if(val) this.initFromParent(1, val); }
  @Input() set reactant2(val: string) { if(val) this.initFromParent(2, val); }

  // Eingabefeld-Werte
  searchTerm1: string = '';
  searchTerm2: string = '';

  // Die final ausgewählten "Chips"
  selectedReactant1: Reactant | null = null;
  selectedReactant2: Reactant | null = null;

  // Dropdown-Vorschläge
  suggestions1: Reactant[] = [];
  suggestions2: Reactant[] = [];

  isDrawModalOpen = false;
  activeReactant: 1 | 2 = 1;
  currentSmilesForModal = '';

  // Simulation der Frontend-Datenbank aller verügbaren Reactants.
  // In der Praxis würdest du diese im ngOnInit() über einen Service laden.
  availableReactants: Reactant[] = [
    { shortName: 'Sodium', smiles: '[Na]', type: 'Metal' },
    { shortName: 'Chlorine', smiles: 'Cl[Cl]', type: 'Halogen' },
    { shortName: 'Hydrogen', smiles: '[H][H]', type: 'Gas' },
    { shortName: 'Oxygen', smiles: 'O=O', type: 'Gas' },
    { shortName: 'Ethanol', smiles: 'CCO', type: 'Solvent' }
  ];

  ngOnInit() {}

  // Wird aufgerufen, wenn Query-Params aus der URL geladen werden
  initFromParent(num: 1 | 2, val: string) {
    const match = this.availableReactants.find(r => r.shortName.toLowerCase() === val.toLowerCase());
    if (match) {
      if (num === 1) this.selectedReactant1 = match;
      else this.selectedReactant2 = match;
    } else {
      if (num === 1) this.searchTerm1 = val;
      else this.searchTerm2 = val;
    }
  }

  // Typahead Filterung
  onSearchInput(num: 1 | 2) {
    const term = num === 1 ? this.searchTerm1 : this.searchTerm2;
    let hits: Reactant[] = [];

    if (term.length > 0) {
      const lowerTerm = term.toLowerCase(); // Für Name und Typ (Case-Insensitive)

      hits = this.availableReactants.filter(r =>
        // Suche nach Name oder Typ (ignorieren Groß-/Kleinschreibung)
        r.shortName.toLowerCase().includes(lowerTerm) ||
        r.type.toLowerCase().includes(lowerTerm) ||
        // Suche nach SMILES (WICHTIG: Kein toLowerCase() verwenden!)
        r.smiles.includes(term)
      );
    }

    if (num === 1) this.suggestions1 = hits;
    else this.suggestions2 = hits;

    this.emitSearch(); // Update parent mit freiem Suchtext
  }

  // Wenn der User im Dropdown klickt
  selectReactant(num: 1 | 2, reactant: Reactant) {
    if (num === 1) {
      this.selectedReactant1 = reactant;
      this.searchTerm1 = '';
      this.suggestions1 = [];
    } else {
      this.selectedReactant2 = reactant;
      this.searchTerm2 = '';
      this.suggestions2 = [];
    }
    this.emitSearch();
  }

  // Einzelnen Chip löschen
  clearReactant(num: 1 | 2) {
    if (num === 1) {
      this.selectedReactant1 = null;
      this.searchTerm1 = '';
      this.suggestions1 = [];
    } else {
      this.selectedReactant2 = null;
      this.searchTerm2 = '';
      this.suggestions2 = [];
    }
    this.emitSearch();
  }

  // Alles löschen (von clearSignal aus dem Parent getriggert)
  clearAll() {
    this.selectedReactant1 = null;
    this.selectedReactant2 = null;
    this.searchTerm1 = '';
    this.searchTerm2 = '';
    this.suggestions1 = [];
    this.suggestions2 = [];
    this.emitSearch();
  }

  emitSearch() {
    this.searchChanged.emit({
      reactant1: this.selectedReactant1 ? this.selectedReactant1.shortName : this.searchTerm1,
      reactant2: this.selectedReactant2 ? this.selectedReactant2.shortName : this.searchTerm2,
    });
  }

  openDraw(num: 1 | 2) {
    this.activeReactant = num;
    const selected = num === 1 ? this.selectedReactant1 : this.selectedReactant2;
    this.currentSmilesForModal = selected ? selected.smiles : (num === 1 ? this.searchTerm1 : this.searchTerm2);
    this.isDrawModalOpen = true;
  }

  onModalSave(newSmiles: string) {
    // Prüfe, ob der gemalte SMILES einem bekannten Molekül entspricht
    const matched = this.availableReactants.find(r => r.smiles === newSmiles);

    if (matched) {
      this.selectReactant(this.activeReactant, matched);
    } else {
      // Wenn es eine komplett neue Substruktur ist, füge es als Text ein
      if (this.activeReactant === 1) {
        this.searchTerm1 = newSmiles;
        this.selectedReactant1 = null;
      } else {
        this.searchTerm2 = newSmiles;
        this.selectedReactant2 = null;
      }
      this.onSearchInput(this.activeReactant);
    }
    this.isDrawModalOpen = false;
  }
}
