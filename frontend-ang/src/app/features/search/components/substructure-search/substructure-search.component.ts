import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DrawMolComponent } from '../../../../shared/draw-mol/draw-mol.component';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ReactantDto } from '../../../reactants/reactant.dto';
import { RDKitService } from '../../../../shared/services/rdkit.service';

export type LogicalOperator = 'AND' | 'OR';

export interface SearchGroup {
  id: number;
  type: 'GROUP';
  operator: LogicalOperator;
  isNot: boolean;
  format: 'SMILES' | 'SMARTS';
  children: (SearchGroup | SearchRule)[];
}

export interface SearchRule {
  id: number;
  type: 'RULE';
  isNot: boolean;
  query: string;
}

@Component({
  selector: 'app-substructure-search',
  templateUrl: './substructure-search.component.html',
  styleUrls: ['./substructure-search.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, DrawMolComponent],
})
export class SubstructureSearchComponent {
  @Input() availableReactants: ReactantDto[] = [];
  @Output() matchedReactants = new EventEmitter<string[]>();

  rootGroup: SearchGroup = {
    id: Date.now(),
    type: 'GROUP',
    operator: 'AND',
    isNot: false,
    format: 'SMARTS',
    children: [],
  };

  isDrawModalOpen = false;
  activeRuleId: number | null = null;
  currentSmilesForModal = '';

  matchedReactantsList: ReactantDto[] = [];

  maxDepth = 3;

  constructor(private rdkitService: RDKitService) {
    this.addRule(this.rootGroup);
  }

  addRule(parentGroup: SearchGroup) {
    parentGroup.children.push({
      id: Date.now(),
      type: 'RULE',
      isNot: false,
      query: '',
    });
  }

  addGroup(parentGroup: SearchGroup, currentDepth: number) {
    if (currentDepth >= this.maxDepth) {
      console.warn('Maximum group depth reached.');
      return;
    }
    parentGroup.children.push({
      id: Date.now(),
      type: 'GROUP',
      operator: 'AND',
      isNot: false,
      format: 'SMARTS',
      children: [{ id: Date.now() + 1, type: 'RULE', isNot: false, query: '' }], // Start group with one rule
    });
  }

  removeNode(id: number, currentGroup: SearchGroup = this.rootGroup): boolean {
    const index = currentGroup.children.findIndex((c) => c.id === id);
    if (index !== -1) {
      currentGroup.children.splice(index, 1);
      return true;
    }
    for (const child of currentGroup.children) {
      if (child.type === 'GROUP') {
        if (this.removeNode(id, child as SearchGroup)) return true;
      }
    }
    return false;
  }

  openDraw(rule: SearchRule) {
    this.activeRuleId = rule.id;
    this.currentSmilesForModal = rule.query;
    this.isDrawModalOpen = true;
  }

  onModalSave(newSmiles: string) {
    const rule = this.findRuleById(this.activeRuleId!, this.rootGroup);
    if (rule) {
      rule.query = newSmiles;
    }
    this.isDrawModalOpen = false;
  }

  private findRuleById(id: number, group: SearchGroup): SearchRule | null {
    for (const child of group.children) {
      if (child.id === id && child.type === 'RULE') return child as SearchRule;
      if (child.type === 'GROUP') {
        const found = this.findRuleById(id, child as SearchGroup);
        if (found) return found;
      }
    }
    return null;
  }

  onSearchSubmit() {
    this.evaluateRulesTree();
  }

  evaluateRulesTree() {
    if (!this.rdkitService.isLoaded) {
      console.warn('RDKit is not loaded yet.');
      return;
    }

    if (this.rootGroup.children.length === 0) {
      this.matchedReactants.emit([]);
      return;
    }

    const matchedObjects = this.availableReactants.filter((reactant) => {
      const targetMol = this.rdkitService.getMol(reactant.smiles);
      if (!targetMol) return false;

      let isMatch = false;

      try {
        isMatch = this.evaluateNode(this.rootGroup, targetMol, this.rootGroup.format);
      } catch (e) {
        console.error('Error evaluating substructure tree for', reactant.shortName, e);
      }

      targetMol.delete();

      return isMatch;
    });

    this.matchedReactantsList = matchedObjects;
    this.matchedReactants.emit(matchedObjects.map((r) => r.uuid));
  }

  private evaluateNode(
    node: SearchGroup | SearchRule,
    targetMol: any,
    currentFormat: 'SMILES' | 'SMARTS',
  ): boolean {
    if (node.type === 'RULE') {
      const rule = node as SearchRule;
      if (!rule.query || rule.query.trim().length === 0) {
        return true;
      }

      const hasSubstructure = this.rdkitService.hasSubstructureMatch(
        targetMol,
        rule.query,
        currentFormat,
      );

      return rule.isNot ? !hasSubstructure : hasSubstructure;
    }

    if (node.type === 'GROUP') {
      const group = node as SearchGroup;

      if (group.children.length === 0) return true;

      let result = this.evaluateNode(group.children[0], targetMol, group.format);

      for (let i = 1; i < group.children.length; i++) {
        const child = group.children[i];

        if (group.operator === 'AND' && !result) break;
        if (group.operator === 'OR' && result) break;

        const childResult = this.evaluateNode(child, targetMol, group.format);

        if (group.operator === 'AND') {
          result = result && childResult;
        } else if (group.operator === 'OR') {
          result = result || childResult;
        }
      }
      return group.isNot ? !result : result;
    }
    return false;
  }

  removeMatchedReactant(reactant: ReactantDto) {
    this.matchedReactantsList = this.matchedReactantsList.filter((r) => r.uuid !== reactant.uuid);
    this.matchedReactants.emit(this.matchedReactantsList.map((r) => r.uuid));
  }

  clearMatchedReactants() {
    this.matchedReactantsList = [];
    this.matchedReactants.emit([]);
  }
}
