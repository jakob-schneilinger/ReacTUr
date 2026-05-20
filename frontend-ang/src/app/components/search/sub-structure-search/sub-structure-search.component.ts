import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Reactant } from '../../../dtos/reactant';

// Das RDKit Objekt vom globalen window scope laden
declare var window: any;

interface SubstructureRule {
  id: number;
  operator: 'AND' | 'OR';
  smiles: string;
}

@Component({
  selector: 'app-substructure-search',
  templateUrl: './sub-structure-search.component.html',
  styleUrls: ['./sub-structure-search.component.scss'],
  standalone: false,
})
export class SubstructureSearchComponent {
  @Input() availableReactants: Reactant[] = [];
  @Output() matchedReactants = new EventEmitter<string[]>();

  rules: SubstructureRule[] = [];

  isDrawModalOpen = false;
  activeRuleId: number | null = null;
  currentSmilesForModal = '';

  constructor() {
    // Direkt mit einer leeren Regel starten
    this.addRule();
  }

  addRule() {
    this.rules.push({
      id: Date.now(),
      operator: 'AND', // Standardmäßig AND
      smiles: ''
    });
  }

  removeRule(id: number) {
    this.rules = this.rules.filter(r => r.id !== id);
    if (this.rules.length === 0) {
      this.addRule(); // Immer mindestens eine Zeile übrig lassen
    }
    this.evaluateRules();
  }

  openDraw(rule: SubstructureRule) {
    this.activeRuleId = rule.id;
    this.currentSmilesForModal = rule.smiles;
    this.isDrawModalOpen = true;
  }

  onModalSave(newSmiles: string) {
    const rule = this.rules.find(r => r.id === this.activeRuleId);
    if (rule) {
      rule.smiles = newSmiles;
    }
    this.isDrawModalOpen = false;
    this.evaluateRules();
  }

  onRuleChange() {
    this.evaluateRules();
  }

  // --- DIE KERN-LOGIK MIT RDKIT ---
  evaluateRules() {
    // Filtere leere Regeln heraus
    const activeRules = this.rules.filter(r => r.smiles.trim().length > 0);

    if (activeRules.length === 0) {
      this.matchedReactants.emit([]);
      return;
    }

    const matchedNames = this.availableReactants.filter(reactant => {
      // 1. Erstelle das Hauptmolekül
      const mol = window.RDKit.get_mol(reactant.smiles);
      if (!mol) return false;

      let finalMatch = false;

      // 2. Logik-Kette von links nach rechts abarbeiten
      for (let i = 0; i < activeRules.length; i++) {
        const rule = activeRules[i];
        const qmol = window.RDKit.get_qmol(rule.smiles);

        // Prüfen, ob die Substruktur im Molekül vorkommt
        const matchObj = JSON.parse(mol.get_substruct_match(qmol));
        const hasSubstructure = Object.keys(matchObj).length > 0;

        qmol.delete(); // Memory Management!

        if (i === 0) {
          // Erste Regel setzt den Startwert
          finalMatch = hasSubstructure;
        } else {
          // Verknüpfung mit vorherigem Ergebnis
          if (rule.operator === 'AND') {
            finalMatch = finalMatch && hasSubstructure;
          } else if (rule.operator === 'OR') {
            finalMatch = finalMatch || hasSubstructure;
          }
        }
      }

      mol.delete(); // Memory Management!
      return finalMatch;
    }).map(r => r.shortName); // Wir wollen nur die Namen (Strings) emitten

    this.matchedReactants.emit(matchedNames);
  }
}
