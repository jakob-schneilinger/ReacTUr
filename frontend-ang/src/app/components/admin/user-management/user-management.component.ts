import { Component, OnInit } from '@angular/core';
import { UserDto } from '../../../dtos/user.dto';
import { UserService } from '../../../services/user.service';
import { NotificationService } from '../../../services/notification.service';

export type status = 'pending' | 'active' | 'deactivate';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  standalone: false,
})
export class UserManagementComponent implements OnInit {
  searchTerm: string = '';

  panels = {
    pending: true,
    active: false,
    deactivated: false,
  };

  users: UserDto[] = [];

  constructor(
    private adminService: UserService,
    private notifications: NotificationService,
  ) {}

  ngOnInit(): void {
    this.adminService.getAllUsers().subscribe({
      next: (users) => (this.users = users),
      error: (err) => this.notifications.error('Error fetching users', err),
    });
  }

  get pendingUsers(): UserDto[] {
    return this.filterUsers('PENDING');
  }

  get activeUsers(): UserDto[] {
    return this.filterUsers('ACTIVATED');
  }

  get deactivatedUsers(): UserDto[] {
    return this.filterUsers('DEACTIVATED');
  }

  private filterUsers(status: 'PENDING' | 'ACTIVATED' | 'DEACTIVATED'): UserDto[] {
    return this.users.filter((user) => {
      const matchesStatus = user.status === status;
      const matchesSearch = this.searchTerm
        ? user.firstName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
          user.lastName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
          user.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
          user.role.toLowerCase().includes(this.searchTerm.toLowerCase())
        : true;

      return matchesStatus && matchesSearch;
    });
  }

  togglePanel(panel: 'pending' | 'active' | 'deactivated'): void {
    this.panels[panel] = !this.panels[panel];
  }

  updateUserStatus(user: UserDto, newStatus: 'ACTIVATED' | 'DEACTIVATED'): void {
    this.adminService.updateUserStatus(user.id, newStatus).subscribe({
      next: (updatedUser) => {
        this.users = this.users.map((u) => (u.id === updatedUser.id ? updatedUser : u));
        this.notifications.success('User status updated successfully');
      },
      error: (err) => {
        this.notifications.error('Error updating user status', err);
      },
    });
  }

  updateUserRole(user: UserDto, event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    const newRole = selectElement.value as 'USER' | 'SUPERVISOR';
    this.adminService.updateUserRole(user.id, newRole).subscribe(
      (updatedUser) => {
        this.users = this.users.map((u) => (u.id === updatedUser.id ? updatedUser : u));
        this.notifications.success('User role updated successfully');
      },
      (err) => {
        this.notifications.error('Error updating user role', err);
      },
    );
  }

  clearUserSearch() {
    this.searchTerm = '';
  }
}
